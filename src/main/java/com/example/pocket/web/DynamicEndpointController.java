package com.example.pocket.web;

import com.example.pocket.service.RecordService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class DynamicEndpointController {
    private static final Logger log = LoggerFactory.getLogger(DynamicEndpointController.class);
    private final RecordService recordService;

    public DynamicEndpointController(RecordService recordService) { this.recordService = recordService; }

    @GetMapping("/api/**")
    public Object handleGet(HttpServletRequest request) {
        String path = request.getRequestURI();
        log.debug("Dynamic GET {}", path);
        return recordService.listViaEndpoint("GET", path);
    }

    @PostMapping("/api/**")
    public Object handlePost(HttpServletRequest request, @RequestBody Map<String,Object> body) {
        String path = request.getRequestURI();
        log.debug("Dynamic POST {} body={}", path, body);
        return recordService.createViaEndpoint("POST", path, body);
    }
}

