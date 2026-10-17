#!/usr/bin/perl
# javaDovecotAdmin sync agent — runs on the mail server, keeps Dovecot's passdb file in sync
# with the dovecot_accounts table.
#
# Flow: consume "account.changed" signals from RabbitMQ (topic exchange, same broker/exchange
# the website publishes to) -> debounce bursts of changes into one run -> read the current
# active account list directly from the app database (MariaDB, MySQL, or PostgreSQL — same
# db.system choice as the website; a narrowly-scoped read-only DB user, separate from the
# website's own credentials — this user can only SELECT from dovecot_accounts, so a compromised
# agent can't read web_accounts or write anything) -> write passdb atomically
# (temp file + rename, never edited in place) -> reload Dovecot -> publish an ok/error status
# back on "sync.status" (same plain-text ok/error + detail convention the website already
# consumes — see SyncStatusRabbitMqConsumer.java).
#
# Config lives in /etc/mailadmin/sync-agent.conf, next to the website's own
# config.properties — same simple key=value format, deliberately not a Perl module so the
# agent has nothing to vet beyond apt packages.
use strict;
use warnings;
use AnyEvent;
use AnyEvent::RabbitMQ;
use DBI;
use File::Temp qw(tempfile);
use POSIX qw(strftime);

my $CONFIG_PATH = $ENV{MAILADMIN_SYNC_AGENT_CONF} || '/etc/mailadmin/sync-agent.conf';
my $RECONNECT_DELAY = 10; # seconds — matches the website's own RabbitMQ consumer backoff

sub log_msg {
    my ($level, $msg) = @_;
    printf STDERR "%s [%s] %s\n", strftime('%Y-%m-%d %H:%M:%S', localtime), $level, $msg;
}

sub load_config {
    my %cfg;
    open my $fh, '<', $CONFIG_PATH or die "Cannot read $CONFIG_PATH: $!\n";
    while (my $line = <$fh>) {
        chomp $line;
        $line =~ s/^\s+|\s+$//g;
        next if $line eq '' || $line =~ /^#/;
        my ($key, $value) = split /=/, $line, 2;
        next unless defined $value;
        $cfg{$key} = $value;
    }
    close $fh;
    return \%cfg;
}

my $cfg = load_config();

# Build the DBI DSN for the configured db.system. MariaDB and MySQL share the DBD::MariaDB
# driver (it speaks both wire protocols); PostgreSQL uses DBD::Pg. The default (no db.system)
# is mariadb, matching installs that predate multi-DB support. Mirrors DbSystem.java on the
# website side — keep the accepted values in sync.
sub build_dsn {
    my $system = lc($cfg->{'db.system'} || 'mariadb');
    my $name = $cfg->{'db.name'};
    my $host = $cfg->{'db.host'};
    if ($system eq 'postgresql') {
        return sprintf('DBI:Pg:dbname=%s;host=%s;port=%s', $name, $host, $cfg->{'db.port'} || 5432);
    }
    # mariadb / mysql
    return sprintf('DBI:MariaDB:database=%s;host=%s;port=%s', $name, $host, $cfg->{'db.port'} || 3306);
}

# --- The actual sync: pull accounts, write passdb, reload Dovecot. Returns (ok, detail). ---
sub run_sync {
    my $dsn = build_dsn();
    my $dbh = eval {
        DBI->connect($dsn, $cfg->{'db.user'}, $cfg->{'db.password'}, { RaiseError => 1, AutoCommit => 1, PrintError => 0 });
    };
    if (!$dbh) {
        return (0, "db connect failed: " . ($@ || $DBI::errstr || 'unknown error'));
    }

    my $rows = eval {
        $dbh->selectall_arrayref(
            "SELECT username, password_hash FROM dovecot_accounts WHERE status = 'active' ORDER BY username",
        );
    };
    if (!$rows) {
        my $err = $@ || $DBI::errstr || 'unknown error';
        $dbh->disconnect;
        return (0, "query failed: $err");
    }
    $dbh->disconnect;

    my $passdb_path = $cfg->{'dovecot.passdb_path'} or return (0, "dovecot.passdb_path not set in $CONFIG_PATH");
    my ($fh, $tmp_path) = eval { tempfile("$passdb_path.XXXXXX", UNLINK => 0) };
    if (!$fh) {
        return (0, "tempfile failed: $@");
    }
    for my $row (@$rows) {
        my ($username, $hash) = @$row;
        print $fh "$username:$hash\n";
    }
    close $fh;
    chmod 0640, $tmp_path;

    unless (rename($tmp_path, $passdb_path)) {
        my $err = $!;
        unlink $tmp_path;
        return (0, "rename into place failed: $err");
    }

    my $reload_cmd = $cfg->{'dovecot.reload_command'} || 'doveadm reload';
    my $reload_output = `$reload_cmd 2>&1`;
    if ($? != 0) {
        chomp $reload_output;
        return (0, "synced " . scalar(@$rows) . " accounts but reload failed: $reload_output");
    }

    return (1, "Synced " . scalar(@$rows) . " accounts");
}

# --- RabbitMQ: consume account.changed (debounced), publish sync.status, reconnect-with-backoff ---
my $rmq;              # the connection object, set once connected (on_success)
my $chan;             # the open channel — publish()/consume() live here, not on $rmq
my $connecting;        # strong reference to the in-flight/connected client object; without
                        # this, nothing keeps it alive between dispatching connect() and its
                        # on_success callback firing, so Perl garbage-collects it mid-handshake
my $debounce_timer;
my $reconnect_timer;

sub publish_status {
    my ($ok, $detail) = @_;
    unless ($chan) {
        log_msg('ERROR', "Can't publish sync.status, not connected: $detail");
        return;
    }
    my $body = ($ok ? "ok" : "error") . ($detail ? "\n$detail" : '');
    # publish() lives on the channel object, not the top-level connection ($rmq) — the
    # connection just negotiates the link; all actual AMQP operations go through a channel.
    $chan->publish(
        exchange    => $cfg->{'rabbitmq.exchange'},
        routing_key => 'sync.status',
        body        => $body,
    );
}

sub schedule_sync {
    undef $debounce_timer;
    my $delay = $cfg->{'sync.debounce_seconds'} || 5;
    $debounce_timer = AnyEvent->timer(
        after => $delay,
        cb    => sub {
            undef $debounce_timer;
            log_msg('INFO', 'Debounce window elapsed, running sync');
            my ($ok, $detail) = run_sync();
            log_msg($ok ? 'INFO' : 'ERROR', $detail);
            publish_status($ok, $detail);
        },
    );
}

sub reconnect_later {
    undef $rmq;
    undef $chan;
    undef $connecting;
    undef $reconnect_timer;
    $reconnect_timer = AnyEvent->timer(
        after => $RECONNECT_DELAY,
        cb    => sub { undef $reconnect_timer; connect_and_consume(); },
    );
}

sub connect_and_consume {
    $connecting = AnyEvent::RabbitMQ->new->load_xml_spec;
    $connecting->connect(
        host       => $cfg->{'rabbitmq.host'},
        port       => $cfg->{'rabbitmq.port'} || 5672,
        vhost      => $cfg->{'rabbitmq.vhost'} || '/',
        user       => $cfg->{'rabbitmq.username'},
        pass       => $cfg->{'rabbitmq.password'},
        timeout    => 10,
        on_success => sub {
            my $ar = shift;
            $rmq = $ar;
            $ar->open_channel(
                on_success => sub {
                    my $channel = shift;
                    $chan = $channel;
                    $channel->declare_exchange(
                        exchange   => $cfg->{'rabbitmq.exchange'},
                        type       => 'topic',
                        durable    => 1,
                        on_success => sub {
                            $channel->declare_queue(
                                queue      => 'mailadmin.sync-agent',
                                durable    => 1,
                                on_success => sub {
                                    my $frame = shift;
                                    my $queue = $frame->method_frame->queue;
                                    $channel->bind_queue(
                                        queue       => $queue,
                                        exchange    => $cfg->{'rabbitmq.exchange'},
                                        # Must match the website's rabbitmq.routingKey (what it
                                        # publishes change signals with). Default account.changed.
                                        routing_key => $cfg->{'rabbitmq.routingKey'} || 'account.changed',
                                        on_success  => sub {
                                            log_msg('INFO', 'Connected to RabbitMQ, waiting for account.changed signals');
                                            # no_ack: a lost signal just means the next change (or a manual
                                            # "Force sync now") triggers the catch-up — not worth the
                                            # complexity of manual ack/requeue for a pure nudge.
                                            $channel->consume(
                                                queue      => $queue,
                                                no_ack     => 1,
                                                on_consume => sub {
                                                    log_msg('INFO', 'Received account.changed, (re)starting debounce window');
                                                    schedule_sync();
                                                },
                                            );
                                        },
                                        on_failure => sub { log_msg('ERROR', "bind_queue failed: @_"); reconnect_later(); },
                                    );
                                },
                                on_failure => sub { log_msg('ERROR', "declare_queue failed: @_"); reconnect_later(); },
                            );
                        },
                        on_failure => sub { log_msg('ERROR', "declare_exchange failed: @_"); reconnect_later(); },
                    );
                },
                on_failure => sub { log_msg('ERROR', "open_channel failed: @_"); reconnect_later(); },
            );
        },
        on_failure => sub {
            log_msg('ERROR', "RabbitMQ connect failed: @_");
            reconnect_later();
        },
        on_close => sub {
            log_msg('ERROR', 'RabbitMQ connection closed unexpectedly');
            reconnect_later();
        },
    );
}

log_msg('INFO', "Starting, config=$CONFIG_PATH");
connect_and_consume();
AnyEvent->condvar->recv;
