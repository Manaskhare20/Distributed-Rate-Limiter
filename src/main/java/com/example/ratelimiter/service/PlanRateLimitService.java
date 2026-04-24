package com.example.ratelimiter.service;

import com.example.ratelimiter.domain.Plan;
import com.example.ratelimiter.domain.PlanRateLimit;
import com.example.ratelimiter.domain.PlanRateLimitId;
import com.example.ratelimiter.dto.PlanRateLimitUpdateRequest;
import com.example.ratelimiter.exception.RateLimitConfigNotFoundException;
import com.example.ratelimiter.repository.PlanRateLimitRepository;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanRateLimitService {

    private static final String PLAN_RATE_LIMIT_CACHE_PREFIX = "plan_rate_limit";

    private final PlanRateLimitRepository planRateLimitRepository;
    private final StringRedisTemplate redisTemplate;

    public PlanRateLimitService(PlanRateLimitRepository planRateLimitRepository,
                                StringRedisTemplate redisTemplate) {
        this.planRateLimitRepository = planRateLimitRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public PlanRateLimit upsertPlanRateLimit(PlanRateLimitUpdateRequest request) {
        PlanRateLimitId id = new PlanRateLimitId(request.getPlan(), request.getEndpoint());
        PlanRateLimit entity = planRateLimitRepository.findById(id)
                .orElseGet(() -> PlanRateLimit.builder()
                        .id(id)
                        .plan(request.getPlan())
                        .build());
        entity.setCapacity(request.getCapacity());
        entity.setRefillRate(request.getRefillRate());

        PlanRateLimit saved = planRateLimitRepository.save(entity);

        // Update Redis cache
        String key = cacheKey(request.getPlan(), request.getEndpoint());
        redisTemplate.opsForHash().put(key, "capacity", String.valueOf(saved.getCapacity()));
        redisTemplate.opsForHash().put(key, "refillRate", String.valueOf(saved.getRefillRate()));
        redisTemplate.expire(key, Duration.ofMinutes(5));

        return saved;
    }

    @Transactional(readOnly = true)
    public PlanRateLimit getEffectiveRateLimit(Plan plan, String endpoint) {
        String key = cacheKey(plan, endpoint);

        Map<Object, Object> cached = redisTemplate.opsForHash().entries(key);
        if (cached != null && cached.containsKey("capacity") && cached.containsKey("refillRate")) {
            long capacity = Long.parseLong((String) cached.get("capacity"));
            long refillRate = Long.parseLong((String) cached.get("refillRate"));
            return PlanRateLimit.builder()
                    .id(new PlanRateLimitId(plan, endpoint))
                    .plan(plan)
                    .capacity(capacity)
                    .refillRate(refillRate)
                    .build();
        }

        Optional<PlanRateLimit> fromDb = planRateLimitRepository.findByPlanAndId_Endpoint(plan, endpoint);
        PlanRateLimit rateLimit = fromDb.orElseThrow(() ->
                new RateLimitConfigNotFoundException("No rate limit configured for plan " + plan + " and endpoint " + endpoint));

        redisTemplate.opsForHash().put(key, "capacity", String.valueOf(rateLimit.getCapacity()));
        redisTemplate.opsForHash().put(key, "refillRate", String.valueOf(rateLimit.getRefillRate()));
        redisTemplate.expire(key, Duration.ofMinutes(5));

        return rateLimit;
    }

    private String cacheKey(Plan plan, String endpoint) {
        return PLAN_RATE_LIMIT_CACHE_PREFIX + ":" + plan.name() + ":" + endpoint;
    }
}

