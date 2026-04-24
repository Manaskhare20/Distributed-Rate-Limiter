package com.example.ratelimiter.repository;

import com.example.ratelimiter.domain.Plan;
import com.example.ratelimiter.domain.PlanDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanDefinitionRepository extends JpaRepository<PlanDefinition, Plan> {
}

