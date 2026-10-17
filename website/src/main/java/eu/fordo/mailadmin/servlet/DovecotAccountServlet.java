package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.DovecotAccountDao;
import eu.fordo.mailadmin.dao.LinkDao;
import eu.fordo.mailadmin.dao.WebAccountDao;
import eu.fordo.mailadmin.model.DovecotAccount;
import eu.fordo.mailadmin.model.WebAccount;
import eu.fordo.mailadmin.service.AuditService;
import eu.fordo.mailadmin.service.MailFolder;
import eu.fordo.mailadmin.service.PasswordService;
import eu.fordo.mailadmin.service.RabbitMqPublisher;
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
            try {
                req.setAttribute("webAccounts", webAccountDao.findAll());
            } catch (SQLException e) {
                SentryReporter.captureException(e);
                throw new ServletException(e);
            }
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
                    req.setAttribute("webAccounts", webAccountDao.findAll());
                    req.getRequestDispatcher("/WEB-INF/jsp/dovecot_account_new.jsp").forward(req, resp);
                    return;
                }
            } else if ("update".equals(action)) {
                update(req);
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
        boolean createDisabled = "true".equals(req.getParameter("createDisabled"));
        String webAccountIdParam = req.getParameter("webAccountId");
        Long webAccountId = (webAccountIdParam == null || webAccountIdParam.isBlank()) ? null : Long.parseLong(webAccountIdParam);

        if (dovecotAccountDao.findByUsername(username).isPresent()) {
            req.setAttribute("errorKey", "mailAccounts.error.duplicate");
            req.setAttribute("submittedUsername", username);
            return;
        }

        // Mailbox folder: use the admin's value if they typed one, else auto-derive from the
        // address. Point several addresses at the same folder to make them share a mailbox.
        String folderParam = req.getParameter("mailFolder");
        String mailFolder = (folderParam == null || folderParam.isBlank())
                ? MailFolder.derive(username) : folderParam.trim();

        String hash = passwordService.hashDovecotPassword(password);
        long id = dovecotAccountDao.create(username, hash, createDisabled ? "disabled" : "active", mailFolder);
        String detail = "username=" + username + " folder=" + mailFolder + (createDisabled ? " (created disabled)" : "");
        auditService.record(req, "dovecot_account_created", "dovecot_account", id, detail);

        if (webAccountId != null) {
            linkDao.setPairing(id, webAccountId);
            auditService.record(req, "link_created", "dovecot_account", id, "webAccountId=" + webAccountId);
        }

        if (!createDisabled) {
            RabbitMqPublisher.publishIfAutoSyncEnabled("auto: created " + username);
        }
    }

    /**
     * One card, one Save: pairing ("Managed by") and active status are submitted together.
     * Each change is applied and audited only if it actually changed, so saving a card with
     * nothing touched is a no-op with no spurious audit entries. The mailbox address itself
     * isn't editable here (there's no rename for a Dovecot account), so it's shown read-only.
     */
    private void update(HttpServletRequest req) throws SQLException {
        long id = Long.parseLong(req.getParameter("id"));
        DovecotAccount account = dovecotAccountDao.findById(id).orElse(null);
        if (account == null) {
            return; // deleted out from under us — the redirect just re-renders the current list
        }
        String username = account.getUsername();

        String webAccountIdParam = req.getParameter("webAccountId");
        Long webAccountId = (webAccountIdParam == null || webAccountIdParam.isBlank()) ? null : Long.parseLong(webAccountIdParam);
        Long currentPair = linkDao.findPairings().get(id);
        if (!java.util.Objects.equals(currentPair, webAccountId)) {
            linkDao.setPairing(id, webAccountId);
            auditService.record(req, webAccountId == null ? "link_removed" : "link_created", "dovecot_account", id,
                    "webAccountId=" + webAccountId);
        }

        // Mailbox folder: blank re-derives from the address (a mailbox always needs a location),
        // so it can't be accidentally cleared. A change triggers a sync since it alters the userdb.
        String folderParam = req.getParameter("mailFolder");
        String newFolder = (folderParam == null || folderParam.isBlank())
                ? MailFolder.derive(username) : folderParam.trim();
        if (!java.util.Objects.equals(account.getMailFolder(), newFolder)) {
            dovecotAccountDao.setMailFolder(id, newFolder);
            auditService.record(req, "dovecot_account_folder_changed", "dovecot_account", id,
                    "username=" + username + " folder=" + newFolder);
            RabbitMqPublisher.publishIfAutoSyncEnabled("auto: folder changed for " + username);
        }

        boolean enabled = "true".equals(req.getParameter("enabled"));
        boolean wasActive = "active".equals(account.getStatus());
        if (wasActive != enabled) {
            String status = enabled ? "active" : "disabled";
            dovecotAccountDao.setStatus(id, status);
            auditService.record(req, enabled ? "dovecot_account_enabled" : "dovecot_account_disabled",
                    "dovecot_account", id, "username=" + username);
            RabbitMqPublisher.publishIfAutoSyncEnabled("auto: " + status + " " + username);
        }
    }

    private void delete(HttpServletRequest req) throws SQLException {
        long id = Long.parseLong(req.getParameter("id"));
        String username = dovecotAccountDao.findById(id).map(DovecotAccount::getUsername).orElse("#" + id);
        dovecotAccountDao.delete(id); // also drops any pairing row via ON DELETE CASCADE
        auditService.record(req, "dovecot_account_deleted", "dovecot_account", id, "username=" + username);
        RabbitMqPublisher.publishIfAutoSyncEnabled("auto: deleted " + username);
    }
}
