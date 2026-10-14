package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.InviteDao;
import eu.fordo.mailadmin.model.Role;
import eu.fordo.mailadmin.service.AuditService;
import eu.fordo.mailadmin.service.SentryReporter;
import eu.fordo.mailadmin.service.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Admin-only: generate single-use invite links that let a new person create a web account for
 * themselves (choosing their own username + password), and list/revoke the ones still pending.
 * The accept side is the public InviteServlet at /invite.
 */
@WebServlet("/admin/web-accounts/invite")
public class InviteAdminServlet extends HttpServlet {

    private static final int DEFAULT_EXPIRY_DAYS = 7;
    private static final int MAX_EXPIRY_DAYS = 90;

    private final InviteDao inviteDao = new InviteDao();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            req.setAttribute("invites", inviteDao.findPending());
            req.setAttribute("inviteBaseUrl", inviteBaseUrl(req));
            req.getRequestDispatcher("/WEB-INF/jsp/web_account_invite.jsp").forward(req, resp);
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("revoke".equals(action)) {
                long id = Long.parseLong(req.getParameter("id"));
                inviteDao.delete(id);
                auditService.record(req, "web_account_invite_revoked", "web_account_invite", id, null);
                resp.sendRedirect(req.getContextPath() + "/admin/web-accounts/invite");
                return;
            }
            // default: generate a new invite
            Role role = "ADMIN".equals(req.getParameter("role")) ? Role.ADMIN : Role.USER;
            String suggestedUsername = req.getParameter("suggestedUsername");
            int days = parseDays(req.getParameter("expiryDays"));
            Instant expiresAt = Instant.now().plus(days, ChronoUnit.DAYS);

            String token = InviteDao.generateToken();
            long id = inviteDao.create(token, role, suggestedUsername, SessionUtil.currentWebAccountId(req), expiresAt);
            auditService.record(req, "web_account_invite_created", "web_account_invite", id,
                    "role=" + role + " expiresInDays=" + days);
            // Forward (not redirect) so we can show the freshly-generated link once, prominently.
            req.setAttribute("generatedLink", inviteBaseUrl(req) + "?token=" + token);
            req.setAttribute("invites", inviteDao.findPending());
            req.setAttribute("inviteBaseUrl", inviteBaseUrl(req));
            req.getRequestDispatcher("/WEB-INF/jsp/web_account_invite.jsp").forward(req, resp);
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
    }

    private static int parseDays(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_EXPIRY_DAYS;
        }
        try {
            int d = Integer.parseInt(raw.trim());
            return Math.max(1, Math.min(MAX_EXPIRY_DAYS, d));
        } catch (NumberFormatException e) {
            return DEFAULT_EXPIRY_DAYS;
        }
    }

    /**
     * Absolute base URL for the public /invite page. Honours X-Forwarded-Proto / X-Forwarded-Host
     * so the link is correct behind the TLS reverse proxy (where Tomcat itself only sees plain
     * http on an internal port), falling back to the request's own scheme/host otherwise.
     */
    private static String inviteBaseUrl(HttpServletRequest req) {
        String proto = header(req, "X-Forwarded-Proto", req.getScheme());
        String host = header(req, "X-Forwarded-Host", null);
        if (host == null) {
            host = req.getServerName();
            int port = req.getServerPort();
            boolean defaultPort = ("https".equals(proto) && port == 443) || ("http".equals(proto) && port == 80);
            if (!defaultPort && port > 0) {
                host = host + ":" + port;
            }
        }
        return proto + "://" + host + req.getContextPath() + "/invite";
    }

    private static String header(HttpServletRequest req, String name, String fallback) {
        String v = req.getHeader(name);
        if (v == null || v.isBlank()) {
            return fallback;
        }
        // X-Forwarded-* can be a comma-separated list; the first value is the original client-facing one.
        int comma = v.indexOf(',');
        return (comma >= 0 ? v.substring(0, comma) : v).trim();
    }
}
