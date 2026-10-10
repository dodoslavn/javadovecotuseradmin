package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.DovecotAccountDao;
import eu.fordo.mailadmin.model.DovecotAccount;
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
import java.util.List;

/**
 * Self-service: a logged-in web account (ADMIN or USER) views, rotates the password of, and
 * enables/disables only the Dovecot account(s) linked to itself. The isLinked() check in
 * every action here is the critical guard — without it a USER could touch another account
 * just by guessing/incrementing the dovecotAccountId form value.
 */
@WebServlet("/my-accounts")
public class MyAccountsServlet extends HttpServlet {

    private final DovecotAccountDao dovecotAccountDao = new DovecotAccountDao();
    private final PasswordService passwordService = new PasswordService();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Long webAccountId = SessionUtil.currentWebAccountId(req);
            List<DovecotAccount> accounts = dovecotAccountDao.findLinkedToWebAccount(webAccountId);
            req.setAttribute("accounts", accounts);
            req.getRequestDispatcher("/WEB-INF/jsp/my_accounts.jsp").forward(req, resp);
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long dovecotAccountId = Long.parseLong(req.getParameter("dovecotAccountId"));
        String action = req.getParameter("action"); // "changePassword" (default, for back-compat with the plain form) | "disable" | "enable"
        Long webAccountId = SessionUtil.currentWebAccountId(req);

        try {
            if (!dovecotAccountDao.isLinked(webAccountId, dovecotAccountId)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Not your account");
                return;
            }
            if ("disable".equals(action)) {
                dovecotAccountDao.setStatus(dovecotAccountId, "disabled");
                auditService.record(req, "dovecot_account_disabled", "dovecot_account", dovecotAccountId, "self-service");
            } else if ("enable".equals(action)) {
                dovecotAccountDao.setStatus(dovecotAccountId, "active");
                auditService.record(req, "dovecot_account_enabled", "dovecot_account", dovecotAccountId, "self-service");
            } else {
                String newPassword = req.getParameter("newPassword");
                String hash = passwordService.hashDovecotPassword(newPassword);
                dovecotAccountDao.updatePasswordHash(dovecotAccountId, hash);
                auditService.record(req, "dovecot_account_password_changed", "dovecot_account", dovecotAccountId, "self-service");
            }
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        } catch (IOException | InterruptedException e) {
            SentryReporter.captureException(e);
            req.setAttribute("errorKey", "myAccounts.error.hashFailed");
            doGet(req, resp);
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/my-accounts");
    }
}
