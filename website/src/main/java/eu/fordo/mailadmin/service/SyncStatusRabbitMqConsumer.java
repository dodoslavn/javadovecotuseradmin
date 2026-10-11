package eu.fordo.mailadmin.service;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import eu.fordo.mailadmin.dao.SyncStatusDao;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Background consumer for the sync agent's reverse notification: once the agent (still not
 * built — see PLAN.md) finishes regenerating /etc/dovecot/passdb, it publishes a short message
 * to this exchange/routing key and this consumer updates sync_status, same data the admin UI
 * reads on "Server configuration".
 *
 * Message body: first line is "ok" or "error" (required), everything after an optional blank
 * line is free-text detail. No JSON — kept deliberately simple so the future Python agent can
 * just send `b"ok\n"` or `b"error\nwhatever went wrong"` with no dependencies.
 *
 * Reuses the same rabbitmq.* credentials as the (not-yet-built) outbound "something changed"
 * publisher — no separate auth mechanism, unlike the HTTP+bearer-token approach this replaced.
 * Deliberately does its own simple reconnect-from-scratch loop rather than relying on
 * amqp-client's automatic recovery, so there's exactly one recovery mechanism to reason about.
 */
public final class SyncStatusRabbitMqConsumer {

    private static final Logger LOG = Logger.getLogger(SyncStatusRabbitMqConsumer.class.getName());
    private static final String QUEUE = "mailadmin.sync-status";
    private static final String ROUTING_KEY = "sync.status";
    private static final long RETRY_DELAY_MS = 10_000;

    private static volatile boolean running = false;
    private static volatile boolean connected = false;
    private static Thread thread;
    private static volatile Connection connection;

    private SyncStatusRabbitMqConsumer() {}

    /** For the "RabbitMQ connection" status shown on Server configuration. */
    public static boolean isConnected() {
        return connected;
    }

    public static synchronized void start() {
        if (running) {
            return;
        }
        if (AppConfig.get("rabbitmq.host", "").isBlank()) {
            LOG.info("RabbitMQ host not configured — sync-status consumer not starting");
            return;
        }
        running = true;
        thread = new Thread(SyncStatusRabbitMqConsumer::runLoop, "rmq-sync-status-consumer");
        thread.setDaemon(true);
        thread.start();
    }

    public static synchronized void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
        closeConnectionQuietly();
    }

    /** Called after RabbitMQ settings are changed via the admin UI, so new credentials take effect immediately. */
    public static synchronized void restart() {
        stop();
        start();
    }

    private static void runLoop() {
        while (running) {
            try {
                connectAndConsume();
            } catch (Exception e) {
                if (running) {
                    LOG.log(Level.WARNING, "RabbitMQ sync-status consumer error, retrying in " + (RETRY_DELAY_MS / 1000) + "s", e);
                }
            }
            closeConnectionQuietly();
            if (!running) {
                return;
            }
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void connectAndConsume() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(AppConfig.get("rabbitmq.host", ""));
        factory.setPort(AppConfig.getInt("rabbitmq.port", 5672));
        factory.setVirtualHost(AppConfig.get("rabbitmq.vhost", "/"));
        factory.setUsername(AppConfig.get("rabbitmq.username", ""));
        factory.setPassword(AppConfig.get("rabbitmq.password", ""));
        factory.setAutomaticRecoveryEnabled(false); // our own runLoop() is the one recovery mechanism

        connection = factory.newConnection("javaDovecotAdmin-sync-status-consumer");
        Channel channel = connection.createChannel();

        String exchange = AppConfig.get("rabbitmq.exchange", "mailacct.events");
        channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, true);
        channel.queueDeclare(QUEUE, true, false, false, null);
        channel.queueBind(QUEUE, exchange, ROUTING_KEY);

        connected = true;
        LOG.info("RabbitMQ sync-status consumer connected (" + factory.getHost() + "), listening on " + QUEUE);

        SyncStatusDao syncStatusDao = new SyncStatusDao();
        DeliverCallback onMessage = (consumerTag, delivery) -> {
            String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
            int nl = body.indexOf('\n');
            String status = (nl >= 0 ? body.substring(0, nl) : body).trim();
            String detail = nl >= 0 ? body.substring(nl + 1).trim() : null;

            if (!"ok".equals(status) && !"error".equals(status)) {
                LOG.warning("Ignoring sync-status message with invalid status: " + status);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                return;
            }
            try {
                syncStatusDao.recordSync(status, (detail == null || detail.isBlank()) ? null : detail);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Failed to record sync status from RabbitMQ message", e);
                SentryReporter.captureException(e);
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true); // requeue, DB may just be down transiently
            }
        };
        channel.basicConsume(QUEUE, false, onMessage, consumerTag -> { });

        // Messages are handled asynchronously by amqp-client's own threads — this thread just
        // stays parked here as long as the connection is alive, so runLoop() doesn't think
        // we're done and immediately try to reconnect.
        while (running && connection.isOpen()) {
            Thread.sleep(1000);
        }
    }

    private static void closeConnectionQuietly() {
        connected = false;
        Connection c = connection;
        connection = null;
        if (c != null) {
            try {
                if (c.isOpen()) {
                    c.close(3000);
                }
            } catch (Exception ignored) {
                // best-effort
            }
        }
    }
}
