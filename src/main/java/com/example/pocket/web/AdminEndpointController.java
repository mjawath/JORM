package com.example.pocket.web;

import com.example.pocket.model.EndpointDef;
import com.example.pocket.service.EndpointService;
import com.example.pocket.web.dto.CreateEndpointRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/endpoints")
public class AdminEndpointController {
    private final EndpointService endpointService;

    public AdminEndpointController(EndpointService endpointService) { this.endpointService = endpointService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointDef create(@RequestBody @Valid CreateEndpointRequest req) {
        return endpointService.create(req.getMethod(), req.getPath(), req.getCollectionName());
    }

    @GetMapping
    public List<EndpointDef> list() { return endpointService.list(); }
}

