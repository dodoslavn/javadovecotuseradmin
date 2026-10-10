package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.SyncStatusDao;
import eu.fordo.mailadmin.service.AppConfig;
import eu.fordo.mailadmin.service.AuditService;
import eu.fordo.mailadmin.service.SentryReporter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * The reverse-notification half of the (deferred, not-yet-built) sync agent: once it exists
 * on the mail server, it POSTs here after each run to report whether regenerating
 * /etc/dovecot/passdb succeeded, so the admin can see "last sync" on Server configuration
 * instead of having to go check the mail host directly.
 *
 * Not session-authenticated (the agent isn't a logged-in web user) — a shared bearer token
 * from AppConfig (sync.token, set via the admin UI) instead. Exempted from AuthFilter's
 * session check via the "/api/" prefix; does its own auth here.
 */
@WebServlet("/api/sync-status")
public class SyncStatusApiServlet extends HttpServlet {

    private final SyncStatusDao syncStatusDao = new SyncStatusDao();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String configuredToken = AppConfig.get("sync.token", "");
        String presented = req.getHeader("Authorization");
        String bearer = presented != null && presented.startsWith("Bearer ") ? presented.substring(7) : null;

        if (configuredToken.isBlank() || bearer == null || !configuredToken.equals(bearer)) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing token");
            return;
        }

        String status = req.getParameter("status");
        String detail = req.getParameter("detail");
        if (!"ok".equals(status) && !"error".equals(status)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "status must be 'ok' or 'error'");
            return;
        }

        try {
            syncStatusDao.recordSync(status, detail);
            auditService.record(req, "sync_reported", "sync_status", null, "status=" + status + (detail != null ? " detail=" + detail : ""));
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("OK");
    }
}
