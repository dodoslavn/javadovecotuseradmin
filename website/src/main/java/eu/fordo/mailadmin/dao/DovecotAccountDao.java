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
        String sql = "SELECT * FROM dovecot_accounts ORDER BY domain, username";
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
                "WHERE link.web_account_id = ? ORDER BY da.domain, da.username";
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

    public long create(String domain, String username, String passwordHash) throws SQLException {
        String sql = "INSERT INTO dovecot_accounts (domain, username, password_hash, status) " +
                "VALUES (?, ?, ?, 'active')";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, domain);
            ps.setString(2, username);
            ps.setString(3, passwordHash);
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

    private DovecotAccount map(ResultSet rs) throws SQLException {
        DovecotAccount a = new DovecotAccount();
        a.setId(rs.getLong("id"));
        a.setDomain(rs.getString("domain"));
        a.setUsername(rs.getString("username"));
        a.setPasswordHash(rs.getString("password_hash"));
        a.setStatus(rs.getString("status"));
        a.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        a.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return a;
    }
}
