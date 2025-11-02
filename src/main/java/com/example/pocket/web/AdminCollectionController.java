package com.example.pocket.web;

import com.example.pocket.model.CollectionDef;
import com.example.pocket.service.CollectionService;
import com.example.pocket.web.dto.CreateCollectionRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/collections")
public class AdminCollectionController {
    private final CollectionService collectionService;

    public AdminCollectionController(CollectionService collectionService) { this.collectionService = collectionService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionDef create(@RequestBody @Valid CreateCollectionRequest req) {
        return collectionService.create(req.getName(), req.getSchemaJson());
    }

    @GetMapping
    public List<CollectionDef> list() { return collectionService.list(); }

    @GetMapping("/{id}")
    public CollectionDef get(@PathVariable Long id) { return collectionService.get(id); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { collectionService.delete(id); }
}

