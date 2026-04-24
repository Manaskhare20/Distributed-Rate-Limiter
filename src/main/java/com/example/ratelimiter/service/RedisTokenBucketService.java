package com.example.ratelimiter.service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisTokenBucketService {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List<Long>> tokenBucketScript;
    private final String keyPrefix;

    public RedisTokenBucketService(
            StringRedisTemplate redisTemplate,
            RedisScript<List<Long>> tokenBucketScript,
            @Value("${rate-limiter.redis.key-prefix:rate_limiter}") String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = tokenBucketScript;
        this.keyPrefix = keyPrefix;
    }

    public TokenBucketResult consumeToken(
            String userId,
            String endpoint,
            long capacity,
            long refillRatePerMinute) {

        String key = keyPrefix + ":" + userId + ":" + endpoint;
        long nowMillis = Instant.now().toEpochMilli();

        List<Long> result = redisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList(key),
                String.valueOf(capacity),
                String.valueOf(refillRatePerMinute),
                String.valueOf(nowMillis)
        );

        if (result == null || result.size() < 3) {
            // In case of unexpected result, be safe and deny.
            return new TokenBucketResult(false, 0, 1000);
        }

        boolean allowed = result.get(0) == 1L;
        long remaining = result.get(1);
        long retryAfterMs = result.get(2);

        return new TokenBucketResult(allowed, remaining, retryAfterMs);
    }
}

