package eu.fordo.mailadmin.model;

import java.time.Instant;

/** Reported by the sync agent (agent/sync-agent.pl) on the mail server over RabbitMQ. */
public class SyncStatus {
    private Instant lastSyncAt;
    private String status; // "ok" | "error", null if never reported
    private String detail;
    private Instant lastRequestSentAt;

    public Instant getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Instant lastSyncAt) { this.lastSyncAt = lastSyncAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public Instant getLastRequestSentAt() { return lastRequestSentAt; }
    public void setLastRequestSentAt(Instant lastRequestSentAt) { this.lastRequestSentAt = lastRequestSentAt; }
}
