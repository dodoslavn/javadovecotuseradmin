package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.WebAccountDao;
import eu.fordo.mailadmin.model.Role;
import eu.fordo.mailadmin.model.WebAccount;
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
import java.util.Optional;

/** Admin-only: list/create/enable/disable website login accounts. */
@WebServlet("/admin/web-accounts")
public class WebAccountServlet extends HttpServlet {

    private final WebAccountDao webAccountDao = new WebAccountDao();
    private final PasswordService passwordService = new PasswordService();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<WebAccount> accounts = webAccountDao.findAll();
            req.setAttribute("accounts", accounts);
            req.getRequestDispatcher("/WEB-INF/jsp/web_accounts.jsp").forward(req, resp);
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        try {
            if ("create".equals(action)) {
                create(req);
                if (req.getAttribute("errorKey") != null) {
                    doGet(req, resp); // re-render the list with the error instead of losing it on redirect
                    return;
                }
            } else if ("disable".equals(action)) {
                long id = Long.parseLong(req.getParameter("id"));
                if (id == SessionUtil.currentWebAccountId(req)) {
                    req.setAttribute("errorKey", "webAccounts.error.selfDisable");
                    doGet(req, resp);
                    return;
                }
                setEnabled(req, false);
            } else if ("enable".equals(action)) {
                setEnabled(req, true);
            }
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
        resp.sendRedirect(req.getContextPath() + "/admin/web-accounts");
    }

    private void create(HttpServletRequest req) throws SQLException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        Role role = Role.valueOf(req.getParameter("role"));

        Optional<WebAccount> existing = webAccountDao.findByUsername(username);
        if (existing.isPresent()) {
            req.setAttribute("errorKey", "webAccounts.error.duplicate");
            return;
        }

        String hash = passwordService.hashWebPassword(password);
        long id = webAccountDao.create(username, hash, role);
        auditService.record(req, "web_account_created", "web_account", id, "username=" + username + " role=" + role);
    }

    private void setEnabled(HttpServletRequest req, boolean enabled) throws SQLException {
        long id = Long.parseLong(req.getParameter("id"));
        webAccountDao.setEnabled(id, enabled);
        auditService.record(req, enabled ? "web_account_enabled" : "web_account_disabled", "web_account", id, null);
    }
}
