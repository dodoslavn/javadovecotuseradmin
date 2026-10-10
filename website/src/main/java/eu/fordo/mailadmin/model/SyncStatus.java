package eu.fordo.mailadmin.model;

import java.time.Instant;

/** Reported by the (deferred, not-yet-built) sync agent on the mail server via POST /api/sync-status. */
public class SyncStatus {
    private Instant lastSyncAt;
    private String status; // "ok" | "error", null if never reported
    private String detail;

    public Instant getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Instant lastSyncAt) { this.lastSyncAt = lastSyncAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
