package eu.fordo.mailadmin.service;

import io.sentry.Sentry;

/**
 * Thin wrapper around the vendored sentry-java SDK (WEB-INF/lib/sentry-java-*.jar — not
 * available via apt, deliberately vendored). DSN comes from AppConfig; if absent, Sentry.init
 * is simply skipped and captureException becomes a no-op.
 */
public final class SentryReporter {

    private static volatile boolean initialized = false;

    private SentryReporter() {}

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        String dsn = AppConfig.get("sentry.dsn", "");
        if (dsn.isBlank()) {
            initialized = true; // no DSN configured -> stay a no-op, don't retry every call
            return;
        }
        Sentry.init(options -> {
            options.setDsn(dsn);
            options.setEnableTracing(false);
        });
        initialized = true;
    }

    /** Called after the DSN is changed via the admin UI, so it takes effect without a restart. */
    public static synchronized void reinit() {
        initialized = false;
        init();
    }

    public static void captureException(Throwable t) {
        init();
        Sentry.captureException(t);
    }
}
