package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.WebAccountDao;
import eu.fordo.mailadmin.service.AuditService;
import eu.fordo.mailadmin.service.PasswordService;
import eu.fordo.mailadmin.service.SentryReporter;
import eu.fordo.mailadmin.service.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Self-service for the currently logged-in WEB login itself (not a mail account): change its
 * password, and pick a preferred UI language that persists across logins/browsers.
 */
@WebServlet("/account")
public class AccountServlet extends HttpServlet {

    private final WebAccountDao webAccountDao = new WebAccountDao();
    private final PasswordService passwordService = new PasswordService();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        forwardWithProfile(req, resp);
    }

    private void forwardWithProfile(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            Long id = SessionUtil.currentWebAccountId(req);
            if (id != null) {
                webAccountDao.findById(id).ifPresent(a -> req.setAttribute("profileLanguage", a.getLanguage()));
            }
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
        req.getRequestDispatcher("/WEB-INF/jsp/account.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("language".equals(req.getParameter("action"))) {
            handleLanguage(req, resp);
            return;
        }
        String newPassword = req.getParameter("newPassword");
        if (newPassword == null || newPassword.length() < 8) {
            req.setAttribute("errorKey", "account.error.tooShort");
            forwardWithProfile(req, resp);
            return;
        }

        try {
            long id = SessionUtil.currentWebAccountId(req);
            webAccountDao.updatePasswordHash(id, passwordService.hashWebPassword(newPassword));
            auditService.record(req, "web_account_password_changed", "web_account", id, "self-service");
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
        req.setAttribute("success", Boolean.TRUE);
        forwardWithProfile(req, resp);
    }

    private void handleLanguage(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Empty selection = clear the saved preference (fall back to browser/cookie).
        String raw = req.getParameter("language");
        String language = (raw == null || raw.isBlank()) ? null : raw.trim();
        if (language != null && !LocaleFilter.isAllowed(language)) {
            req.setAttribute("errorKey", "account.language.error");
            forwardWithProfile(req, resp);
            return;
        }
        try {
            long id = SessionUtil.currentWebAccountId(req);
            webAccountDao.setLanguage(id, language);
            auditService.record(req, "web_account_language_changed", "web_account", id,
                    language == null ? "cleared" : language);
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
        // Apply immediately: a concrete choice wins now; clearing lets the resolver fall through
        // on the next request (session still holds the old value, but the profile no longer forces it).
        if (language != null) {
            LocaleFilter.remember(req, resp, language);
        }
        req.setAttribute("languageSuccess", Boolean.TRUE);
        forwardWithProfile(req, resp);
    }
}
