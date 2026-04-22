package com.zosh.seed;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeedRunRepository extends JpaRepository<SeedRun, Long> {
    Optional<SeedRun> findByScenarioKey(String scenarioKey);
}
