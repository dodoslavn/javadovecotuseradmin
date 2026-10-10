package eu.fordo.mailadmin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * The underlying table is many-to-many, but the UI only ever offers "which single web
 * account is this mail account paired to" (a dropdown, not a multi-select) — setPairing()
 * enforces that by clearing any existing links for the mail account before adding the new
 * one, so the table never actually accumulates more than one row per dovecot_account_id in
 * practice even though the schema itself doesn't enforce it.
 */
public class LinkDao {

    /** dovecot_account_id -> the one web_account_id it's paired with (accounts with no pairing are simply absent). */
    public Map<Long, Long> findPairings() throws SQLException {
        String sql = "SELECT dovecot_account_id, web_account_id FROM web_account_dovecot_account";
        Map<Long, Long> result = new HashMap<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getLong("dovecot_account_id"), rs.getLong("web_account_id"));
            }
        }
        return result;
    }

    public void setPairing(long dovecotAccountId, Long webAccountId) throws SQLException {
        try (Connection c = Database.getConnection()) {
            try (PreparedStatement clear = c.prepareStatement(
                    "DELETE FROM web_account_dovecot_account WHERE dovecot_account_id = ?")) {
                clear.setLong(1, dovecotAccountId);
                clear.executeUpdate();
            }
            if (webAccountId != null) {
                try (PreparedStatement insert = c.prepareStatement(
                        "INSERT INTO web_account_dovecot_account (web_account_id, dovecot_account_id) VALUES (?, ?)")) {
                    insert.setLong(1, webAccountId);
                    insert.setLong(2, dovecotAccountId);
                    insert.executeUpdate();
                }
            }
        }
    }
}
