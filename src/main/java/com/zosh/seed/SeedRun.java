package com.zosh.seed;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "seed_runs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_seed_run_scenario", columnNames = "scenarioKey")
})
@Getter
@Setter
@NoArgsConstructor
public class SeedRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String scenarioKey;

    @Column(nullable = false)
    private String volume;

    @Column(nullable = false)
    private Integer days;

    @Column(nullable = false)
    private Long randomSeed;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
