package com.example.pocket.service;

import com.example.pocket.exception.NotFoundException;
import com.example.pocket.model.CollectionDef;
import com.example.pocket.model.EndpointDef;
import com.example.pocket.repository.EndpointRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EndpointService {
    private final EndpointRepository repository;
    private final CollectionService collectionService;

    public EndpointService(EndpointRepository repository, CollectionService collectionService) {
        this.repository = repository;
        this.collectionService = collectionService;
    }

    public EndpointDef create(String method, String path, String collectionName) {
        method = method.toUpperCase();
        if (!path.startsWith("/")) path = "/" + path;
        CollectionDef coll = collectionService.getByName(collectionName);
        if (repository.findByMethodAndPath(method, path).isPresent()) {
            throw new IllegalArgumentException("Endpoint already exists: " + method + " " + path);
        }
        return repository.create(method, path, coll.getId());
    }

    public List<EndpointDef> list() { return repository.findAll(); }

    public EndpointDef find(String method, String path) {
        return repository.findByMethodAndPath(method.toUpperCase(), path)
                .orElseThrow(() -> new NotFoundException("Endpoint not found: " + method + " " + path));
    }
}

