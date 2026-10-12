package eu.fordo.mailadmin.dao;

import eu.fordo.mailadmin.model.SyncStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/** Single-row table (id always 1) — see sql/schema.sql / SchemaInstaller. */
public class SyncStatusDao {

    public SyncStatus get() throws SQLException {
        String sql = "SELECT last_sync_at, status, detail, last_request_sent_at FROM sync_status WHERE id = 1";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            SyncStatus s = new SyncStatus();
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("last_sync_at");
                s.setLastSyncAt(ts == null ? null : ts.toInstant());
                s.setStatus(rs.getString("status"));
                s.setDetail(rs.getString("detail"));
                Timestamp requestTs = rs.getTimestamp("last_request_sent_at");
                s.setLastRequestSentAt(requestTs == null ? null : requestTs.toInstant());
            }
            return s;
        }
    }

    public void recordSync(String status, String detail) throws SQLException {
        String sql = "INSERT INTO sync_status (id, last_sync_at, status, detail) VALUES (1, CURRENT_TIMESTAMP, ?, ?) " +
                "ON DUPLICATE KEY UPDATE last_sync_at = CURRENT_TIMESTAMP, status = VALUES(status), detail = VALUES(detail)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, detail);
            ps.executeUpdate();
        }
    }

    /** Called when the website itself successfully publishes the "something changed" signal — see RabbitMqPublisher. */
    public void recordRequestSent() throws SQLException {
        String sql = "INSERT INTO sync_status (id, last_request_sent_at) VALUES (1, CURRENT_TIMESTAMP) " +
                "ON DUPLICATE KEY UPDATE last_request_sent_at = CURRENT_TIMESTAMP";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }
}
