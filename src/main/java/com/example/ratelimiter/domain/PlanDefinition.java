package com.example.ratelimiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents valid plans in the system.
 * Contains two rows: NORMAL, PREMIUM.
 */
@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
public class PlanDefinition {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, unique = true)
    private Plan plan;
}

