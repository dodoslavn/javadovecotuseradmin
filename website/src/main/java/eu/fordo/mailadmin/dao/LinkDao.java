package eu.fordo.mailadmin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LinkDao {

    public void link(long webAccountId, long dovecotAccountId) throws SQLException {
        String sql = "INSERT IGNORE INTO web_account_dovecot_account (web_account_id, dovecot_account_id) VALUES (?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, webAccountId);
            ps.setLong(2, dovecotAccountId);
            ps.executeUpdate();
        }
    }

    public void unlink(long webAccountId, long dovecotAccountId) throws SQLException {
        String sql = "DELETE FROM web_account_dovecot_account WHERE web_account_id = ? AND dovecot_account_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, webAccountId);
            ps.setLong(2, dovecotAccountId);
            ps.executeUpdate();
        }
    }
}
