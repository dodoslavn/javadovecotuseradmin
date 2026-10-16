package eu.fordo.mailadmin.dao;

import eu.fordo.mailadmin.model.Role;
import eu.fordo.mailadmin.model.WebAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WebAccountDao {

    public Optional<WebAccount> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM web_accounts WHERE username = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<WebAccount> findById(long id) throws SQLException {
        String sql = "SELECT * FROM web_accounts WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<WebAccount> findAll() throws SQLException {
        String sql = "SELECT * FROM web_accounts ORDER BY username";
        List<WebAccount> result = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    public long create(String username, String passwordHash, Role role) throws SQLException {
        String sql = "INSERT INTO web_accounts (username, password_hash, role, enabled) VALUES (?, ?, ?, TRUE)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, role.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void setEnabled(long id, boolean enabled) throws SQLException {
        String sql = "UPDATE web_accounts SET enabled = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, enabled);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void updatePasswordHash(long id, String passwordHash) throws SQLException {
        String sql = "UPDATE web_accounts SET password_hash = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void setRole(long id, Role role) throws SQLException {
        String sql = "UPDATE web_accounts SET role = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, role.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void setLanguage(long id, String language) throws SQLException {
        String sql = "UPDATE web_accounts SET language = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, language);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void setUsername(long id, String username) throws SQLException {
        String sql = "UPDATE web_accounts SET username = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM web_accounts WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private WebAccount map(ResultSet rs) throws SQLException {
        WebAccount a = new WebAccount();
        a.setId(rs.getLong("id"));
        a.setUsername(rs.getString("username"));
        a.setPasswordHash(rs.getString("password_hash"));
        a.setRole(Role.valueOf(rs.getString("role")));
        a.setEnabled(rs.getBoolean("enabled"));
        a.setLanguage(rs.getString("language"));
        a.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return a;
    }
}
