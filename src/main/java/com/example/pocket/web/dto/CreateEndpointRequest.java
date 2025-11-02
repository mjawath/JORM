package com.example.pocket.web.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateEndpointRequest {
    @NotBlank
    private String method; // GET or POST supported for now
    @NotBlank
    private String path;
    @NotBlank
    private String collectionName;

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
}

