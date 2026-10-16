package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.SyncStatusDao;
import eu.fordo.mailadmin.model.SyncStatus;
import eu.fordo.mailadmin.service.AppConfig;
import eu.fordo.mailadmin.service.AuditService;
import eu.fordo.mailadmin.service.DbSystem;
import eu.fordo.mailadmin.service.RabbitMqPublisher;
import eu.fordo.mailadmin.service.SentryReporter;
import eu.fordo.mailadmin.service.SessionUtil;
import eu.fordo.mailadmin.service.SyncStatusRabbitMqConsumer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin-only. MySQL/MariaDB settings are shown read-only — the app can't safely rewrite its
 * own DB credentials before it can even connect, so those stay host-edited-file-only.
 * RabbitMQ and Sentry settings ARE editable here and get written back to
 * /etc/mailadmin/config.properties (not a DB table — see AppConfig) via AppConfig.update(),
 * which rewrites only the matching lines and leaves everything else in the file untouched.
 *
 * Also shows the last report from the sync agent (agent/sync-agent.pl, runs on the mail
 * server), delivered via RabbitMQ — see SyncStatusRabbitMqConsumer — reusing these same
 * rabbitmq.* credentials rather than a separate auth mechanism. The "Auto-sync" toggle here
 * controls whether mail-account changes publish the "something changed" signal automatically
 * (RabbitMqPublisher.publishIfAutoSyncEnabled(), called from DovecotAccountServlet and
 * MyAccountsServlet) or only when "Force sync now" is clicked.
 */
@WebServlet("/admin/server-config")
public class ServerConfigServlet extends HttpServlet {

    private final AuditService auditService = new AuditService();
    private final SyncStatusDao syncStatusDao = new SyncStatusDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("configFilePath", AppConfig.configFilePath());
        req.setAttribute("configFilePresent", AppConfig.isConfigFilePresent());

        String dbUrl = AppConfig.get("db.url", "");
        req.setAttribute("dbUrl", dbUrl);
        req.setAttribute("dbSystemName", DbSystem.displayNameForUrl(dbUrl));
        req.setAttribute("dbUser", AppConfig.get("db.user", ""));
        req.setAttribute("dbPasswordSet", !AppConfig.get("db.password", "").isBlank());

        req.setAttribute("rmqHost", AppConfig.get("rabbitmq.host", ""));
        req.setAttribute("rmqPort", AppConfig.getInt("rabbitmq.port", 5672));
        req.setAttribute("rmqVhost", AppConfig.get("rabbitmq.vhost", ""));
        req.setAttribute("rmqUsername", AppConfig.get("rabbitmq.username", ""));
        req.setAttribute("rmqPasswordSet", !AppConfig.get("rabbitmq.password", "").isBlank());
        req.setAttribute("rmqExchange", AppConfig.get("rabbitmq.exchange", ""));
        req.setAttribute("rmqRoutingKey", AppConfig.get("rabbitmq.routingKey", ""));
        req.setAttribute("rmqQueue", AppConfig.get("rabbitmq.queue", ""));

        req.setAttribute("sentryDsn", AppConfig.get("sentry.dsn", ""));
        req.setAttribute("rmqConnected", SyncStatusRabbitMqConsumer.isConnected());
        req.setAttribute("autoSyncEnabled", "true".equals(AppConfig.get("sync.autosync", "false")));

        try {
            req.setAttribute("syncStatus", syncStatusDao.get());
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }

        req.getRequestDispatcher("/WEB-INF/jsp/server_config.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if ("sentry".equals(action)) {
            updateSentry(req, resp);
            return;
        }
        if ("forceSync".equals(action)) {
            forceSync(req, resp);
            return;
        }
        if ("autosync".equals(action)) {
            updateAutoSync(req, resp);
            return;
        }
        updateRabbitMq(req, resp);
    }

    private void forceSync(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String actor = SessionUtil.currentUsername(req);
        try {
            RabbitMqPublisher.publishSyncRequest("manual trigger by " + actor);
            auditService.record(req, "sync_requested", "server_config", null, "manual trigger");
            req.setAttribute("forceSyncResult", "success");
        } catch (Exception e) {
            SentryReporter.captureException(e);
            req.setAttribute("forceSyncResult", "error");
        }
        doGet(req, resp); // forward, not redirect, so the one-time result message survives
    }

    private void updateAutoSync(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean enabled = "true".equals(req.getParameter("enabled"));
        try {
            AppConfig.update(Map.of("sync.autosync", String.valueOf(enabled)));
            auditService.record(req, "sync_autosync_updated", "server_config", null, enabled ? "enabled" : "disabled");
        } catch (Exception e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
        resp.sendRedirect(req.getContextPath() + "/admin/server-config");
    }

    private void updateRabbitMq(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String host = req.getParameter("host");
        String port = req.getParameter("port");
        String vhost = req.getParameter("vhost");
        String username = req.getParameter("username");
        String password = req.getParameter("password"); // blank = keep the existing password unchanged
        String exchange = req.getParameter("exchange");
        String routingKey = req.getParameter("routingKey");
        String queue = req.getParameter("queue");

        Map<String, String> updates = new LinkedHashMap<>();
        updates.put("rabbitmq.host", host == null ? "" : host);
        updates.put("rabbitmq.port", port == null || port.isBlank() ? "5672" : port);
        updates.put("rabbitmq.vhost", vhost == null ? "" : vhost);
        updates.put("rabbitmq.username", username == null ? "" : username);
        updates.put("rabbitmq.exchange", exchange == null ? "" : exchange);
        updates.put("rabbitmq.routingKey", routingKey == null ? "" : routingKey);
        updates.put("rabbitmq.queue", queue == null ? "" : queue);
        if (password != null && !password.isBlank()) {
            updates.put("rabbitmq.password", password);
        }

        try {
            AppConfig.update(updates);
            SyncStatusRabbitMqConsumer.restart(); // picks up new host/credentials/exchange immediately
            auditService.record(req, "rabbitmq_config_updated", "server_config", null,
                    "host=" + host + " port=" + port + " vhost=" + vhost + " exchange=" + exchange + " routingKey=" + routingKey + " queue=" + queue
                            + (password != null && !password.isBlank() ? " (password rotated)" : ""));
        } catch (Exception e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
        resp.sendRedirect(req.getContextPath() + "/admin/server-config");
    }

    private void updateSentry(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String dsn = req.getParameter("dsn");

        try {
            AppConfig.update(Map.of("sentry.dsn", dsn == null ? "" : dsn));
            SentryReporter.reinit(); // picks up the new DSN (or turns reporting off) without a restart
            auditService.record(req, "sentry_dsn_updated", "server_config", null, dsn == null || dsn.isBlank() ? "cleared" : "set");
        } catch (Exception e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
        resp.sendRedirect(req.getContextPath() + "/admin/server-config");
    }
}
