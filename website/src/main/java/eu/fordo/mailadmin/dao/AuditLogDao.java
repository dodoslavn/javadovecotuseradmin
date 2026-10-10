package eu.fordo.mailadmin.dao;

import eu.fordo.mailadmin.model.AuditLogEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDao {

    public void record(Long actorWebAccountId, String action, String targetType, Long targetId, String detail) throws SQLException {
        String sql = "INSERT INTO audit_log (actor_web_account_id, action, target_type, target_id, detail) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (actorWebAccountId == null) {
                ps.setNull(1, java.sql.Types.BIGINT);
            } else {
                ps.setLong(1, actorWebAccountId);
            }
            ps.setString(2, action);
            ps.setString(3, targetType);
            if (targetId == null) {
                ps.setNull(4, java.sql.Types.BIGINT);
            } else {
                ps.setLong(4, targetId);
            }
            ps.setString(5, detail);
            ps.executeUpdate();
        }
    }

    public List<AuditLogEntry> findRecent(int limit) throws SQLException {
        String sql = "SELECT al.*, wa.username AS actor_username FROM audit_log al " +
                "LEFT JOIN web_accounts wa ON wa.id = al.actor_web_account_id " +
                "ORDER BY al.at DESC LIMIT ?";
        List<AuditLogEntry> result = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLogEntry e = new AuditLogEntry();
                    e.setId(rs.getLong("id"));
                    long actorId = rs.getLong("actor_web_account_id");
                    e.setActorWebAccountId(rs.wasNull() ? null : actorId);
                    e.setActorUsername(rs.getString("actor_username"));
                    e.setAction(rs.getString("action"));
                    e.setTargetType(rs.getString("target_type"));
                    long targetId = rs.getLong("target_id");
                    e.setTargetId(rs.wasNull() ? null : targetId);
                    e.setDetail(rs.getString("detail"));
                    e.setAt(rs.getTimestamp("at").toInstant());
                    result.add(e);
                }
            }
        }
        return result;
    }
}
