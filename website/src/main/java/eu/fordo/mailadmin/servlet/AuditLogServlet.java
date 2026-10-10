package eu.fordo.mailadmin.servlet;

import eu.fordo.mailadmin.dao.AuditLogDao;
import eu.fordo.mailadmin.model.AuditLogEntry;
import eu.fordo.mailadmin.service.SentryReporter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/** Admin-only: read-only audit trail. */
@WebServlet("/admin/audit-log")
public class AuditLogServlet extends HttpServlet {

    private final AuditLogDao auditLogDao = new AuditLogDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<AuditLogEntry> entries = auditLogDao.findRecent(200);
            req.setAttribute("entries", entries);
            req.getRequestDispatcher("/WEB-INF/jsp/audit_log.jsp").forward(req, resp);
        } catch (SQLException e) {
            SentryReporter.captureException(e);
            throw new ServletException(e);
        }
    }
}
