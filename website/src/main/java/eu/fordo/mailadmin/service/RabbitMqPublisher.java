package eu.fordo.mailadmin.service;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import eu.fordo.mailadmin.dao.SyncStatusDao;

import java.nio.charset.StandardCharsets;

/**
 * Publishes the outbound "something changed" signal to the sync agent — a lightweight
 * trigger only, never account data. Called from the admin's manual "Force sync now" button,
 * and (when the "sync.autosync" config flag is on) automatically after mail-account changes
 * that actually affect what the agent syncs — see publishIfAutoSyncEnabled().
 *
 * Opens a short-lived connection per publish rather than keeping one open, since even with
 * autosync this is still a low-frequency action (one admin action at a time, not a hot path)
 * — simpler than sharing state with the long-lived consumer connection in
 * SyncStatusRabbitMqConsumer.
 */
public final class RabbitMqPublisher {

    /** Default routing key for the outbound "something changed" signal; overridable via rabbitmq.routingKey. */
    private static final String DEFAULT_ROUTING_KEY = "account.changed";

    private RabbitMqPublisher() {}

    /**
     * Fire-and-forget: publishes only if the admin has turned on "Auto-sync" in Server
     * configuration, and never throws — a RabbitMQ hiccup here must not fail the mail-account
     * action that triggered it. Callers that already know they want to publish unconditionally
     * (the manual "Force sync now" button) should call publishSyncRequest() directly instead.
     */
    public static void publishIfAutoSyncEnabled(String reason) {
        if (!"true".equals(AppConfig.get("sync.autosync", "false"))) {
            return;
        }
        try {
            publishSyncRequest(reason);
        } catch (Exception e) {
            SentryReporter.captureException(e);
        }
    }

    public static void publishSyncRequest(String reason) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(AppConfig.get("rabbitmq.host", ""));
        factory.setPort(AppConfig.getInt("rabbitmq.port", 5672));
        factory.setVirtualHost(AppConfig.get("rabbitmq.vhost", "/"));
        factory.setUsername(AppConfig.get("rabbitmq.username", ""));
        factory.setPassword(AppConfig.get("rabbitmq.password", ""));

        try (Connection connection = factory.newConnection("tomcatdovecotuseradmin-publisher");
             Channel channel = connection.createChannel()) {
            String exchange = AppConfig.get("rabbitmq.exchange", "mailacct.events");
            String routingKey = AppConfig.get("rabbitmq.routingKey", DEFAULT_ROUTING_KEY);
            if (routingKey.isBlank()) {
                routingKey = DEFAULT_ROUTING_KEY;
            }
            channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, true);
            String body = (reason == null || reason.isBlank()) ? "changed" : reason;
            channel.basicPublish(exchange, routingKey, null, body.getBytes(StandardCharsets.UTF_8));
        }

        // Record separately from the try-with-resources above: the publish itself already
        // succeeded by this point, so a DB hiccup recording it must not make the caller think
        // the publish failed.
        try {
            new SyncStatusDao().recordRequestSent();
        } catch (Exception e) {
            SentryReporter.captureException(e);
        }
    }
}
