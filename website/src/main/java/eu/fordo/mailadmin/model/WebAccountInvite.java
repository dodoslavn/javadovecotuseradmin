package eu.fordo.mailadmin.model;

import java.time.Instant;

/** A single-use, time-limited link that lets a new person create a web account for themselves. */
public class WebAccountInvite {
    private long id;
    private String token;
    private Role role;
    private String suggestedUsername;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant usedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getSuggestedUsername() { return suggestedUsername; }
    public void setSuggestedUsername(String suggestedUsername) { this.suggestedUsername = suggestedUsername; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    public boolean isUsed() { return usedAt != null; }
    public boolean isExpired() { return expiresAt != null && Instant.now().isAfter(expiresAt); }
    /** Can still be accepted: neither used nor expired. */
    public boolean isPending() { return !isUsed() && !isExpired(); }
}
