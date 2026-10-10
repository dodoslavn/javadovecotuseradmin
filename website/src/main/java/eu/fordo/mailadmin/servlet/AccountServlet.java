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

/** Self-service: change the password of the currently logged-in WEB login itself (not a mail account). */
@WebServlet("/account")
public class AccountServlet extends HttpServlet {

    private final WebAccountDao webAccountDao = new WebAccountDao();
    private final PasswordService passwordService = new PasswordService();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/account.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String newPassword = req.getParameter("newPassword");
        if (newPassword == null || newPassword.length() < 8) {
            req.setAttribute("errorKey", "account.error.tooShort");
            req.getRequestDispatcher("/WEB-INF/jsp/account.jsp").forward(req, resp);
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
        req.getRequestDispatcher("/WEB-INF/jsp/account.jsp").forward(req, resp);
    }
}
