package com.example.pocket.model;

import java.time.Instant;

public class CollectionDef {
    private Long id;
    private String name;
    private String schemaJson; // optional JSON describing fields
    private Instant createdAt;
    private Instant updatedAt;

    public CollectionDef() {}

    public CollectionDef(Long id, String name, String schemaJson, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.schemaJson = schemaJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSchemaJson() { return schemaJson; }
    public void setSchemaJson(String schemaJson) { this.schemaJson = schemaJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

