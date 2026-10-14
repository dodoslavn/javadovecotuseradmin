package eu.fordo.mailadmin.dao;

import eu.fordo.mailadmin.model.Role;
import eu.fordo.mailadmin.model.WebAccountInvite;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class InviteDao {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    /** 256 bits of entropy, URL-safe — infeasible to guess, so no rate-limiting on the accept endpoint is needed. */
    public static String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    public long create(String token, Role role, String suggestedUsername, Long createdByWebAccountId, Instant expiresAt) throws SQLException {
        String sql = "INSERT INTO web_account_invites (token, role, suggested_username, created_by_web_account_id, expires_at) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, token);
            ps.setString(2, role.name());
            if (suggestedUsername == null || suggestedUsername.isBlank()) {
                ps.setNull(3, java.sql.Types.VARCHAR);
            } else {
                ps.setString(3, suggestedUsername.trim());
            }
            if (createdByWebAccountId == null) {
                ps.setNull(4, java.sql.Types.BIGINT);
            } else {
                ps.setLong(4, createdByWebAccountId);
            }
            ps.setTimestamp(5, Timestamp.from(expiresAt));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public Optional<WebAccountInvite> findByToken(String token) throws SQLException {
        String sql = "SELECT * FROM web_account_invites WHERE token = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Still-pending invites (not used, not expired), newest first — for the admin list. */
    public List<WebAccountInvite> findPending() throws SQLException {
        String sql = "SELECT * FROM web_account_invites WHERE used_at IS NULL AND expires_at > CURRENT_TIMESTAMP ORDER BY created_at DESC";
        List<WebAccountInvite> result = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    /**
     * Atomically mark an invite used, but only if it's still pending — returns true if this
     * caller won the race. Guards against two people accepting the same link at once.
     */
    public boolean markUsedIfPending(long id, long usedWebAccountId) throws SQLException {
        String sql = "UPDATE web_account_invites SET used_at = CURRENT_TIMESTAMP, used_web_account_id = ? " +
                "WHERE id = ? AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, usedWebAccountId);
            ps.setLong(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM web_account_invites WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private WebAccountInvite map(ResultSet rs) throws SQLException {
        WebAccountInvite i = new WebAccountInvite();
        i.setId(rs.getLong("id"));
        i.setToken(rs.getString("token"));
        i.setRole(Role.valueOf(rs.getString("role")));
        i.setSuggestedUsername(rs.getString("suggested_username"));
        i.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        i.setExpiresAt(rs.getTimestamp("expires_at").toInstant());
        Timestamp used = rs.getTimestamp("used_at");
        i.setUsedAt(used == null ? null : used.toInstant());
        return i;
    }
}
