package com.se2030.dental.dto;

import java.time.Instant;

public class HealthResponse {
    private String status;
    private String database;
    private String timestamp;

    public HealthResponse() {
        this.timestamp = Instant.now().toString();
    }

    public HealthResponse(String status, String database) {
        this();
        this.status = status;
        this.database = database;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
