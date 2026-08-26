package com.canteen.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@Tag(name = "Health", description = "Service health check")
public class HealthController {

    @GetMapping("/api/health")
    @Operation(summary = "Public health check")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "smart-canteen-backend",
                "timestamp", LocalDateTime.now()
        );
    }
}