package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.DovecotAccountDao;
import eu.fordo.mailadmin.dao.LinkDao;
import eu.fordo.mailadmin.dao.WebAccountDao;
import eu.fordo.mailadmin.model.DovecotAccount;
import eu.fordo.mailadmin.model.WebAccount;
import eu.fordo.mailadmin.service.AuditService;
import eu.fordo.mailadmin.service.SentryReporter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/** Admin-only: link/unlink which web account owns which Dovecot mail account(s). */
@WebServlet("/admin/links")
public class LinkServlet extends HttpServlet {

    private final WebAccountDao webAccountDao = new WebAccountDao();
    private final DovecotAccountDao dovecotAccountDao = new DovecotAccountDao();
    private final LinkDao linkDao = new LinkDao();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<WebAccount> webAccounts = webAccountDao.findAll();
            List<DovecotAccount> dovecotAccounts = dovecotAccountDao.findAll();
            req.setAttribute("webAccounts", webAccounts);
            req.setAttribute("dovecotAccounts", dovecotAccounts);
            req.getRequestDispatcher("/WEB-INF/jsp/links.jsp").forward(req, resp);
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        long webAccountId = Long.parseLong(req.getParameter("webAccountId"));
        long dovecotAccountId = Long.parseLong(req.getParameter("dovecotAccountId"));
        try {
            if ("link".equals(action)) {
                linkDao.link(webAccountId, dovecotAccountId);
                auditService.record(req, "link_created", "link", dovecotAccountId,
                        "webAccountId=" + webAccountId + " dovecotAccountId=" + dovecotAccountId);
            } else if ("unlink".equals(action)) {
                linkDao.unlink(webAccountId, dovecotAccountId);
                auditService.record(req, "link_removed", "link", dovecotAccountId,
                        "webAccountId=" + webAccountId + " dovecotAccountId=" + dovecotAccountId);
            }
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
        resp.sendRedirect(req.getContextPath() + "/admin/links");
    }
}
