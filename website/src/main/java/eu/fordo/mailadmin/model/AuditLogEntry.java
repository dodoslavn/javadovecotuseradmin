package eu.fordo.mailadmin.model;

import java.time.Instant;

public class AuditLogEntry {
    private long id;
    private Long actorWebAccountId;
    private String actorUsername; // joined in for display
    private String action;
    private String targetType;
    private Long targetId;
    private String detail;
    private Instant at;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Long getActorWebAccountId() { return actorWebAccountId; }
    public void setActorWebAccountId(Long actorWebAccountId) { this.actorWebAccountId = actorWebAccountId; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }
}
