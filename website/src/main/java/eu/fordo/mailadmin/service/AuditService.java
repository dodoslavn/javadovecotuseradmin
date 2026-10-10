package eu.fordo.mailadmin.service;

import eu.fordo.mailadmin.dao.AuditLogDao;
import jakarta.servlet.http.HttpServletRequest;

import java.sql.SQLException;

/** Convenience wrapper so servlets don't have to pull the actor id out of the session by hand each time. */
public class AuditService {

    private final AuditLogDao auditLogDao = new AuditLogDao();

    public void record(HttpServletRequest req, String action, String targetType, Long targetId, String detail) throws SQLException {
        Long actorId = SessionUtil.currentWebAccountId(req);
        auditLogDao.record(actorId, action, targetType, targetId, detail);
    }
}
