package com.example.ratelimiter.service;

import com.example.ratelimiter.domain.Plan;
import com.example.ratelimiter.domain.PlanRateLimit;
import com.example.ratelimiter.domain.User;
import com.example.ratelimiter.dto.RateLimitCheckRequest;
import com.example.ratelimiter.dto.RateLimitCheckResponse;
import com.example.ratelimiter.exception.NotFoundException;
import com.example.ratelimiter.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RateLimitService {

    private final UserRepository userRepository;
    private final PlanRateLimitService planRateLimitService;
    private final RedisTokenBucketService redisTokenBucketService;

    public RateLimitService(UserRepository userRepository,
                            PlanRateLimitService planRateLimitService,
                            RedisTokenBucketService redisTokenBucketService) {
        this.userRepository = userRepository;
        this.planRateLimitService = planRateLimitService;
        this.redisTokenBucketService = redisTokenBucketService;
    }

    @Transactional(readOnly = true)
    public RateLimitCheckResponse checkRateLimit(RateLimitCheckRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.getUserId()));

        Plan plan = user.getPlan();
        PlanRateLimit rateLimit = planRateLimitService.getEffectiveRateLimit(plan, request.getEndpoint());

        TokenBucketResult bucketResult = redisTokenBucketService.consumeToken(
                request.getUserId(),
                request.getEndpoint(),
                rateLimit.getCapacity(),
                rateLimit.getRefillRate()
        );

        return new RateLimitCheckResponse(
                bucketResult.isAllowed(),
                bucketResult.getRemainingTokens(),
                bucketResult.getRetryAfterMs()
        );
    }
}

