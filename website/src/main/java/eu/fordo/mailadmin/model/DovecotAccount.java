package eu.fordo.mailadmin.model;

import java.time.Instant;

public class DovecotAccount {
    private long id;
    private String domain;
    private String username;
    private String passwordHash;
    private int uid;
    private int gid;
    private String homeDir;
    private String status; // "active" | "disabled"
    private Instant createdAt;
    private Instant updatedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public int getUid() { return uid; }
    public void setUid(int uid) { this.uid = uid; }

    public int getGid() { return gid; }
    public void setGid(int gid) { this.gid = gid; }

    public String getHomeDir() { return homeDir; }
    public void setHomeDir(String homeDir) { this.homeDir = homeDir; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
