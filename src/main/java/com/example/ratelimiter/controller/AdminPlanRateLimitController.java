package com.example.ratelimiter.controller;

import com.example.ratelimiter.domain.PlanRateLimit;
import com.example.ratelimiter.dto.PlanRateLimitUpdateRequest;
import com.example.ratelimiter.service.PlanRateLimitService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/plan-rate-limit")
public class AdminPlanRateLimitController {

    private final PlanRateLimitService planRateLimitService;

    public AdminPlanRateLimitController(PlanRateLimitService planRateLimitService) {
        this.planRateLimitService = planRateLimitService;
    }

    @PutMapping
    public ResponseEntity<PlanRateLimit> upsert(@Valid @RequestBody PlanRateLimitUpdateRequest request) {
        PlanRateLimit updated = planRateLimitService.upsertPlanRateLimit(request);
        return ResponseEntity.ok(updated);
    }
}

