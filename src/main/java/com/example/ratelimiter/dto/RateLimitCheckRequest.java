package com.example.ratelimiter.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RateLimitCheckRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String endpoint;
}

