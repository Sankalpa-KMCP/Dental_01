package com.se2030.dental.controller;

import com.se2030.dental.dto.HealthResponse;
import com.se2030.dental.exception.DatabaseUnavailableException;
import com.se2030.dental.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final HealthService healthService;

    @Autowired
    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        if (!healthService.isDatabaseHealthy()) {
            throw new DatabaseUnavailableException("Database connection check failed");
        }
        HealthResponse response = new HealthResponse("UP", "UP");
        return ResponseEntity.ok(response);
    }
}
