package com.example.ratelimiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "plan_rate_limits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanRateLimit {

    @EmbeddedId
    private PlanRateLimitId id;

    
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, insertable = false, updatable = false)
    private Plan plan;

    @Column(name = "capacity", nullable = false)
    private long capacity;

    /**
     * Refill rate in tokens per minute.
     */
    @Column(name = "refill_rate", nullable = false)
    private long refillRate;
}

