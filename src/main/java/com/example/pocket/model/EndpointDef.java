package com.example.pocket.model;

import java.time.Instant;

public class EndpointDef {
    private Long id;
    private String method; // GET, POST, etc (uppercase)
    private String path;   // e.g., /api/tasks
    private Long collectionId;
    private Instant createdAt;
    private Instant updatedAt;

    public EndpointDef() {}

    public EndpointDef(Long id, String method, String path, Long collectionId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.method = method;
        this.path = path;
        this.collectionId = collectionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Long getCollectionId() { return collectionId; }
    public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

