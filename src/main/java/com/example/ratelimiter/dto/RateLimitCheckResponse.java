package com.example.ratelimiter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RateLimitCheckResponse {

    private final boolean allowed;
    private final long remainingTokens;
    private final long retryAfterMs;
}

