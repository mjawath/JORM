package com.example.pocket.model;

import java.time.Instant;

public class RecordEntry {
    private String id; // UUID
    private Long collectionId;
    private String dataJson;
    private Instant createdAt;
    private Instant updatedAt;

    public RecordEntry() {}

    public RecordEntry(String id, Long collectionId, String dataJson, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.collectionId = collectionId;
        this.dataJson = dataJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getCollectionId() { return collectionId; }
    public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }
    public String getDataJson() { return dataJson; }
    public void setDataJson(String dataJson) { this.dataJson = dataJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

