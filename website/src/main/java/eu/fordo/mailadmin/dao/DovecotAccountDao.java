package eu.fordo.mailadmin.dao;

import eu.fordo.mailadmin.model.DovecotAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DovecotAccountDao {

    public List<DovecotAccount> findAll() throws SQLException {
        String sql = "SELECT * FROM dovecot_accounts ORDER BY username";
        List<DovecotAccount> result = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    public List<DovecotAccount> findLinkedToWebAccount(long webAccountId) throws SQLException {
        String sql = "SELECT da.* FROM dovecot_accounts da " +
                "JOIN web_account_dovecot_account link ON link.dovecot_account_id = da.id " +
                "WHERE link.web_account_id = ? ORDER BY da.username";
        List<DovecotAccount> result = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, webAccountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    public Optional<DovecotAccount> findById(long id) throws SQLException {
        String sql = "SELECT * FROM dovecot_accounts WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<DovecotAccount> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM dovecot_accounts WHERE username = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** True if the given web account is linked to the given dovecot account — used for self-service access checks. */
    public boolean isLinked(long webAccountId, long dovecotAccountId) throws SQLException {
        String sql = "SELECT 1 FROM web_account_dovecot_account WHERE web_account_id = ? AND dovecot_account_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, webAccountId);
            ps.setLong(2, dovecotAccountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long create(String username, String passwordHash, String status) throws SQLException {
        String sql = "INSERT INTO dovecot_accounts (username, password_hash, status) VALUES (?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, status);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void updatePasswordHash(long id, String passwordHash) throws SQLException {
        String sql = "UPDATE dovecot_accounts SET password_hash = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void setStatus(long id, String status) throws SQLException {
        String sql = "UPDATE dovecot_accounts SET status = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /** Also removes any pairing row via ON DELETE CASCADE on web_account_dovecot_account. */
    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM dovecot_accounts WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private DovecotAccount map(ResultSet rs) throws SQLException {
        DovecotAccount a = new DovecotAccount();
        a.setId(rs.getLong("id"));
        a.setUsername(rs.getString("username"));
        a.setPasswordHash(rs.getString("password_hash"));
        a.setStatus(rs.getString("status"));
        a.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        a.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return a;
    }
}
