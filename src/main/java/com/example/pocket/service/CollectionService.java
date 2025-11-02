package com.example.pocket.service;

import com.example.pocket.exception.NotFoundException;
import com.example.pocket.model.CollectionDef;
import com.example.pocket.repository.CollectionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CollectionService {
    private final CollectionRepository repository;

    public CollectionService(CollectionRepository repository) { this.repository = repository; }

    public CollectionDef create(String name, String schemaJson) {
        if (repository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Collection already exists: " + name);
        }
        return repository.create(name, schemaJson);
    }

    public List<CollectionDef> list() { return repository.findAll(); }

    public CollectionDef get(Long id) { return repository.findById(id).orElseThrow(() -> new NotFoundException("Collection not found: " + id)); }

    public CollectionDef getByName(String name) { return repository.findByName(name).orElseThrow(() -> new NotFoundException("Collection not found: " + name)); }

    public void delete(Long id) { if (!repository.delete(id)) throw new NotFoundException("Collection not found: " + id); }
}

