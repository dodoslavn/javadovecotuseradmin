package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.SyncStatusDao;
import eu.fordo.mailadmin.model.SyncStatus;
import eu.fordo.mailadmin.service.AppConfig;
import eu.fordo.mailadmin.service.AuditService;
import eu.fordo.mailadmin.service.SentryReporter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Admin-only. MySQL/MariaDB settings are shown read-only — the app can't safely rewrite its
 * own DB credentials before it can even connect, so those stay host-edited-file-only.
 * RabbitMQ settings ARE editable here and get written back to
 * /etc/mailadmin/config.properties (not a DB table — see AppConfig) via AppConfig.update(),
 * which rewrites only the matching lines and leaves everything else in the file untouched.
 *
 * Also shows the last report from the (deferred, not-yet-built) sync agent — see
 * SyncStatusApiServlet — and lets the admin generate the bearer token that agent will need.
 */
@WebServlet("/admin/server-config")
public class ServerConfigServlet extends HttpServlet {

    private final AuditService auditService = new AuditService();
    private final SyncStatusDao syncStatusDao = new SyncStatusDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("configFilePath", AppConfig.configFilePath());
        req.setAttribute("configFilePresent", AppConfig.isConfigFilePresent());

        req.setAttribute("dbUrl", AppConfig.get("db.url", ""));
        req.setAttribute("dbUser", AppConfig.get("db.user", ""));
        req.setAttribute("dbPasswordSet", !AppConfig.get("db.password", "").isBlank());

        req.setAttribute("rmqHost", AppConfig.get("rabbitmq.host", ""));
        req.setAttribute("rmqPort", AppConfig.getInt("rabbitmq.port", 5672));
        req.setAttribute("rmqVhost", AppConfig.get("rabbitmq.vhost", ""));
        req.setAttribute("rmqUsername", AppConfig.get("rabbitmq.username", ""));
        req.setAttribute("rmqPasswordSet", !AppConfig.get("rabbitmq.password", "").isBlank());
        req.setAttribute("rmqExchange", AppConfig.get("rabbitmq.exchange", ""));

        req.setAttribute("sentryDsn", AppConfig.get("sentry.dsn", ""));

        req.setAttribute("syncTokenSet", !AppConfig.get("sync.token", "").isBlank());
        // "newSyncToken" is left untouched here — it's only ever present on the request when
        // doGet() is called from regenerateSyncToken() right after creating one, so the JSP
        // can show it exactly once.

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
        if ("regenerateSyncToken".equals(action)) {
            regenerateSyncToken(req, resp);
            return;
        }
        if ("sentry".equals(action)) {
            updateSentry(req, resp);
            return;
        }
        updateRabbitMq(req, resp);
    }

    private void updateRabbitMq(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String host = req.getParameter("host");
        String port = req.getParameter("port");
        String vhost = req.getParameter("vhost");
        String username = req.getParameter("username");
        String password = req.getParameter("password"); // blank = keep the existing password unchanged
        String exchange = req.getParameter("exchange");

        Map<String, String> updates = new LinkedHashMap<>();
        updates.put("rabbitmq.host", host == null ? "" : host);
        updates.put("rabbitmq.port", port == null || port.isBlank() ? "5672" : port);
        updates.put("rabbitmq.vhost", vhost == null ? "" : vhost);
        updates.put("rabbitmq.username", username == null ? "" : username);
        updates.put("rabbitmq.exchange", exchange == null ? "" : exchange);
        if (password != null && !password.isBlank()) {
            updates.put("rabbitmq.password", password);
        }

        try {
            AppConfig.update(updates);
            auditService.record(req, "rabbitmq_config_updated", "server_config", null,
                    "host=" + host + " port=" + port + " vhost=" + vhost + " exchange=" + exchange
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

    private void regenerateSyncToken(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        String newToken = sb.toString();

        try {
            AppConfig.update(Map.of("sync.token", newToken));
            auditService.record(req, "sync_token_regenerated", "server_config", null, null);
        } catch (Exception e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }

        // Shown once, directly after regeneration, since there's no other way to retrieve it
        // later — the admin needs to copy this into the sync agent's own config.
        req.setAttribute("newSyncToken", newToken);
        doGet(req, resp);
    }
}
