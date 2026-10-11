package eu.fordo.mailadmin.service;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;

/**
 * Publishes the outbound "something changed" signal to the sync agent (still not built — see
 * PLAN.md) — a lightweight trigger only, never account data. Currently only ever called from
 * the admin's manual "Force sync now" button; nothing publishes automatically on account
 * create/update/delete yet.
 *
 * Opens a short-lived connection per publish rather than keeping one open, since this is a
 * low-frequency manual action — simpler than sharing state with the long-lived consumer
 * connection in SyncStatusRabbitMqConsumer.
 */
public final class RabbitMqPublisher {

    private static final String ROUTING_KEY = "account.changed";

    private RabbitMqPublisher() {}

    public static void publishSyncRequest(String reason) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(AppConfig.get("rabbitmq.host", ""));
        factory.setPort(AppConfig.getInt("rabbitmq.port", 5672));
        factory.setVirtualHost(AppConfig.get("rabbitmq.vhost", "/"));
        factory.setUsername(AppConfig.get("rabbitmq.username", ""));
        factory.setPassword(AppConfig.get("rabbitmq.password", ""));

        try (Connection connection = factory.newConnection("javaDovecotAdmin-publisher");
             Channel channel = connection.createChannel()) {
            String exchange = AppConfig.get("rabbitmq.exchange", "mailacct.events");
            channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC, true);
            String body = (reason == null || reason.isBlank()) ? "changed" : reason;
            channel.basicPublish(exchange, ROUTING_KEY, null, body.getBytes(StandardCharsets.UTF_8));
        }
    }
}
