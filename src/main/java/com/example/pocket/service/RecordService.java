package com.example.pocket.service;

import com.example.pocket.model.CollectionDef;
import com.example.pocket.model.EndpointDef;
import com.example.pocket.model.RecordEntry;
import com.example.pocket.repository.RecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.SpecVersion;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecordService {
    private final RecordRepository recordRepository;
    private final EndpointService endpointService;
    private final CollectionService collectionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RecordService(RecordRepository recordRepository, EndpointService endpointService, CollectionService collectionService) {
        this.recordRepository = recordRepository;
        this.endpointService = endpointService;
        this.collectionService = collectionService;
    }

    public Map<String, Object> createViaEndpoint(String method, String path, Map<String, Object> payload) {
        EndpointDef ep = endpointService.find(method, path);
        CollectionDef coll = collectionService.get(ep.getCollectionId());
        // Schema validation
        if (coll.getSchemaJson() != null && !coll.getSchemaJson().isBlank()) {
            try {
                JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
                JsonSchema schema = factory.getSchema(coll.getSchemaJson());
                JsonNode node = objectMapper.valueToTree(payload);
                if (!(node instanceof ObjectNode)) {
                    throw new IllegalArgumentException("Payload must be a JSON object");
                }
                java.util.Set<ValidationMessage> errors = schema.validate(node);
                if (!errors.isEmpty()) {
                    throw new IllegalArgumentException("Schema validation failed: " + errors.toString());
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Schema validation error: " + e.getMessage(), e);
            }
        }
        String json;
        try { json = objectMapper.writeValueAsString(payload); } catch (JsonProcessingException e) { throw new IllegalArgumentException("Invalid JSON payload"); }
        RecordEntry entry = recordRepository.create(coll.getId(), json);
        return buildResponse(entry);
    }

    public List<Map<String, Object>> listViaEndpoint(String method, String path) {
        EndpointDef ep = endpointService.find(method, path); // ensures endpoint exists
        List<RecordEntry> list = recordRepository.listByCollection(ep.getCollectionId());
        return list.stream().map(this::buildResponse).collect(Collectors.toList());
    }

    private Map<String, Object> buildResponse(RecordEntry e) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", e.getId());
        try {
            JsonNode node = objectMapper.readTree(e.getDataJson());
            if (node.isObject()) {
                node.fields().forEachRemaining(f -> map.put(f.getKey(), objectMapper.convertValue(f.getValue(), Object.class)));
            } else {
                map.put("data", objectMapper.convertValue(node, Object.class));
            }
        } catch (Exception ex) {
            map.put("raw", e.getDataJson());
        }
        return map;
    }
}
