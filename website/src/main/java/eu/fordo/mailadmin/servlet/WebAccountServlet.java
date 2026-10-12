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

/** Admin-only: list/create/enable/disable website login accounts, reset their password, change their role, or rename them. */
@WebServlet({"/admin/web-accounts", "/admin/web-accounts/new"})
public class WebAccountServlet extends HttpServlet {

    private final WebAccountDao webAccountDao = new WebAccountDao();
    private final PasswordService passwordService = new PasswordService();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getServletPath().endsWith("/new")) {
            req.getRequestDispatcher("/WEB-INF/jsp/web_account_new.jsp").forward(req, resp);
            return;
        }
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
                    req.getRequestDispatcher("/WEB-INF/jsp/web_account_new.jsp").forward(req, resp);
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
            } else if ("update".equals(action)) {
                update(req, resp);
                if (req.getAttribute("errorKey") != null) {
                    return; // update() already re-rendered on failure
                }
            }
            if (req.getAttribute("errorKey") != null) {
                doGet(req, resp); // re-render the list with the error instead of losing it on redirect
                return;
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
            req.setAttribute("submittedUsername", username);
            return;
        }

        String hash = passwordService.hashWebPassword(password);
        long id = webAccountDao.create(username, hash, role);
        auditService.record(req, "web_account_created", "web_account", id, "username=" + username + " role=" + role);
    }

    private void setEnabled(HttpServletRequest req, boolean enabled) throws SQLException {
        long id = Long.parseLong(req.getParameter("id"));
        String username = webAccountDao.findById(id).map(WebAccount::getUsername).orElse("#" + id);
        webAccountDao.setEnabled(id, enabled);
        auditService.record(req, enabled ? "web_account_enabled" : "web_account_disabled", "web_account", id, "username=" + username);
    }

    /**
     * One card, one Save button: rename + role + (optional) password rotation all land in a
     * single form submission rather than three separate inline-forms each with their own
     * button. Blank password field = leave the current password unchanged, same convention
     * as the RabbitMQ password field on Server configuration. Enable/disable stays a
     * separate, single-click form (see "disable"/"enable" above) since that's the one action
     * an admin would want to fire immediately without also touching name/role/password.
     */
    private void update(HttpServletRequest req, HttpServletResponse resp) throws SQLException, ServletException, IOException {
        long id = Long.parseLong(req.getParameter("id"));
        String newUsername = req.getParameter("newUsername");
        String newPassword = req.getParameter("newPassword");
        Role role = Role.valueOf(req.getParameter("role"));

        if (newUsername == null || newUsername.isBlank()) {
            req.setAttribute("errorKey", "webAccounts.error.usernameRequired");
            doGet(req, resp);
            return;
        }
        newUsername = newUsername.trim();

        Optional<WebAccount> existing = webAccountDao.findByUsername(newUsername);
        if (existing.isPresent() && existing.get().getId() != id) {
            req.setAttribute("errorKey", "webAccounts.error.duplicate");
            doGet(req, resp);
            return;
        }

        if (newPassword != null && !newPassword.isBlank() && newPassword.length() < 8) {
            req.setAttribute("errorKey", "webAccounts.error.passwordTooShort");
            doGet(req, resp);
            return;
        }

        Optional<WebAccount> self = webAccountDao.findById(id);
        boolean isSelf = id == SessionUtil.currentWebAccountId(req);
        if (isSelf && self.isPresent() && self.get().getRole() != role) {
            req.setAttribute("errorKey", "webAccounts.error.selfRoleChange");
            doGet(req, resp);
            return;
        }

        StringBuilder detail = new StringBuilder("newUsername=").append(newUsername).append(" role=").append(role);
        webAccountDao.setUsername(id, newUsername);
        webAccountDao.setRole(id, role);
        if (newPassword != null && !newPassword.isBlank()) {
            webAccountDao.updatePasswordHash(id, passwordService.hashWebPassword(newPassword));
            detail.append(" (password rotated)");
        }
        auditService.record(req, "web_account_updated", "web_account", id, detail.toString());
    }
}
