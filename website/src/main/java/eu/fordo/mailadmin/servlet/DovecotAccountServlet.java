package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.DovecotAccountDao;
import eu.fordo.mailadmin.model.DovecotAccount;
import eu.fordo.mailadmin.service.AuditService;
import eu.fordo.mailadmin.service.PasswordService;
import eu.fordo.mailadmin.service.SentryReporter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Admin-only: list/create/disable Dovecot mail accounts. */
@WebServlet("/admin/dovecot-accounts")
public class DovecotAccountServlet extends HttpServlet {

    private final DovecotAccountDao dovecotAccountDao = new DovecotAccountDao();
    private final PasswordService passwordService = new PasswordService();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<DovecotAccount> accounts = dovecotAccountDao.findAll();
            req.setAttribute("accounts", accounts);
            req.getRequestDispatcher("/WEB-INF/jsp/dovecot_accounts.jsp").forward(req, resp);
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
                setStatus(req, "disabled");
            } else if ("enable".equals(action)) {
                setStatus(req, "active");
            }
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        } catch (IOException | InterruptedException e) {
            SentryReporter.captureException(e);
            req.setAttribute("errorKey", "mailAccounts.error.hashFailed");
            doGet(req, resp);
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/admin/dovecot-accounts");
    }

    private void create(HttpServletRequest req) throws SQLException, IOException, InterruptedException {
        String domain = req.getParameter("domain");
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        int uid = Integer.parseInt(req.getParameter("uid"));
        int gid = Integer.parseInt(req.getParameter("gid"));
        String homeDir = req.getParameter("homeDir");

        Optional<DovecotAccount> existing = dovecotAccountDao.findAll().stream()
                .filter(a -> a.getDomain().equals(domain) && a.getUsername().equals(username))
                .findFirst();
        if (existing.isPresent()) {
            req.setAttribute("errorKey", "mailAccounts.error.duplicate");
            return;
        }

        String hash = passwordService.hashDovecotPassword(password);
        long id = dovecotAccountDao.create(domain, username, hash, uid, gid, homeDir);
        auditService.record(req, "dovecot_account_created", "dovecot_account", id, "domain=" + domain + " username=" + username);
    }

    private void setStatus(HttpServletRequest req, String status) throws SQLException {
        long id = Long.parseLong(req.getParameter("id"));
        dovecotAccountDao.setStatus(id, status);
        auditService.record(req, "dovecot_account_" + ("disabled".equals(status) ? "disabled" : "enabled"),
                "dovecot_account", id, null);
    }
}
