package com.example.ratelimiter.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenBucketResult {

    private final boolean allowed;
    private final long remainingTokens;
    private final long retryAfterMs;
}

