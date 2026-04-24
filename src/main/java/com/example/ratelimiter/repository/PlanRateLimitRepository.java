package com.example.ratelimiter.repository;

import com.example.ratelimiter.domain.Plan;
import com.example.ratelimiter.domain.PlanRateLimit;
import com.example.ratelimiter.domain.PlanRateLimitId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRateLimitRepository extends JpaRepository<PlanRateLimit, PlanRateLimitId> {

    Optional<PlanRateLimit> findByPlanAndId_Endpoint(Plan plan, String endpoint);
}

