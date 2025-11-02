package com.example.pocket.web.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateCollectionRequest {
    @NotBlank
    private String name;
    private String schemaJson;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSchemaJson() { return schemaJson; }
    public void setSchemaJson(String schemaJson) { this.schemaJson = schemaJson; }
}

