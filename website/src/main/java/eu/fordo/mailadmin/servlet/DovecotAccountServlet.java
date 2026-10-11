package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.DovecotAccountDao;
import eu.fordo.mailadmin.dao.LinkDao;
import eu.fordo.mailadmin.dao.WebAccountDao;
import eu.fordo.mailadmin.model.DovecotAccount;
import eu.fordo.mailadmin.model.WebAccount;
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
import java.util.Map;

/** Admin-only: list/create/disable Dovecot mail accounts, and pair each one to a web account. */
@WebServlet({"/admin/dovecot-accounts", "/admin/dovecot-accounts/new"})
public class DovecotAccountServlet extends HttpServlet {

    private final DovecotAccountDao dovecotAccountDao = new DovecotAccountDao();
    private final WebAccountDao webAccountDao = new WebAccountDao();
    private final LinkDao linkDao = new LinkDao();
    private final PasswordService passwordService = new PasswordService();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getServletPath().endsWith("/new")) {
            req.getRequestDispatcher("/WEB-INF/jsp/dovecot_account_new.jsp").forward(req, resp);
            return;
        }
        try {
            List<DovecotAccount> accounts = dovecotAccountDao.findAll();
            List<WebAccount> webAccounts = webAccountDao.findAll();
            Map<Long, Long> pairings = linkDao.findPairings();
            req.setAttribute("accounts", accounts);
            req.setAttribute("webAccounts", webAccounts);
            req.setAttribute("pairings", pairings);
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
                    req.getRequestDispatcher("/WEB-INF/jsp/dovecot_account_new.jsp").forward(req, resp);
                    return;
                }
            } else if ("disable".equals(action)) {
                setStatus(req, "disabled");
            } else if ("enable".equals(action)) {
                setStatus(req, "active");
            } else if ("pair".equals(action)) {
                pair(req);
            } else if ("delete".equals(action)) {
                delete(req);
            }
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        } catch (IOException | InterruptedException e) {
            SentryReporter.captureException(e);
            req.setAttribute("errorKey", "mailAccounts.error.hashFailed");
            if ("create".equals(action)) {
                req.getRequestDispatcher("/WEB-INF/jsp/dovecot_account_new.jsp").forward(req, resp);
            } else {
                doGet(req, resp);
            }
            return;
        }
        resp.sendRedirect(req.getContextPath() + "/admin/dovecot-accounts");
    }

    private void create(HttpServletRequest req) throws SQLException, IOException, InterruptedException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (dovecotAccountDao.findByUsername(username).isPresent()) {
            req.setAttribute("errorKey", "mailAccounts.error.duplicate");
            req.setAttribute("submittedUsername", username);
            return;
        }

        String hash = passwordService.hashDovecotPassword(password);
        long id = dovecotAccountDao.create(username, hash);
        auditService.record(req, "dovecot_account_created", "dovecot_account", id, "username=" + username);
    }

    private void setStatus(HttpServletRequest req, String status) throws SQLException {
        long id = Long.parseLong(req.getParameter("id"));
        dovecotAccountDao.setStatus(id, status);
        auditService.record(req, "dovecot_account_" + ("disabled".equals(status) ? "disabled" : "enabled"),
                "dovecot_account", id, null);
    }

    private void delete(HttpServletRequest req) throws SQLException {
        long id = Long.parseLong(req.getParameter("id"));
        String username = dovecotAccountDao.findById(id).map(DovecotAccount::getUsername).orElse("#" + id);
        dovecotAccountDao.delete(id); // also drops any pairing row via ON DELETE CASCADE
        auditService.record(req, "dovecot_account_deleted", "dovecot_account", id, "username=" + username);
    }

    private void pair(HttpServletRequest req) throws SQLException {
        long dovecotAccountId = Long.parseLong(req.getParameter("dovecotAccountId"));
        String webAccountIdParam = req.getParameter("webAccountId");
        Long webAccountId = (webAccountIdParam == null || webAccountIdParam.isBlank()) ? null : Long.parseLong(webAccountIdParam);

        linkDao.setPairing(dovecotAccountId, webAccountId);
        auditService.record(req, webAccountId == null ? "link_removed" : "link_created", "dovecot_account", dovecotAccountId,
                "webAccountId=" + webAccountId);
    }
}
