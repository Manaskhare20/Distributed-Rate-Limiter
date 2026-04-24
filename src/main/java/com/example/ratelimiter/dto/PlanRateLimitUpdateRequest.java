package com.example.ratelimiter.dto;

import com.example.ratelimiter.domain.Plan;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanRateLimitUpdateRequest {

    @NotNull
    private Plan plan;

    @NotBlank
    private String endpoint;

    @Min(1)
    private long capacity;

    /**
     * Refill rate in tokens per minute.
     */
    @Min(1)
    private long refillRate;
}

