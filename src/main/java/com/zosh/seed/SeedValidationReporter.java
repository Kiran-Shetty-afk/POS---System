package com.zosh.seed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;

@Component
@Slf4j
public class SeedValidationReporter {

    public void report(SeedScenarioConfig config, SeedMetrics metrics) {
        log.info("=========== Historical Seed Completed ===========");
        log.info("Scenario key: {}", config.getScenarioKey());
        log.info("Volume: {}, days: {}, randomSeed: {}", config.getVolume(), config.getDays(), config.getRandomSeed());
        log.info("Range: {} -> {}", metrics.getRangeStart(), metrics.getRangeEnd());
        log.info("Stores: {}, branches: {}", metrics.getStoresCreated(), metrics.getBranchesCreated());
        log.info("Categories: {}, products: {}, inventory rows: {}",
                metrics.getCategoriesCreated(),
                metrics.getProductsCreated(),
                metrics.getInventoriesCreated());
        log.info("Customers: {}, cashiers: {}", metrics.getCustomersCreated(), metrics.getCashiersCreated());
        log.info("Orders: {}, refunds: {}, shifts: {}",
                metrics.getOrdersCreated(),
                metrics.getRefundsCreated(),
                metrics.getShiftReportsCreated());

        log.info("Top branches by seeded revenue:");
        metrics.getSalesByBranch().entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .forEach(entry -> log.info(" - {} => {}", entry.getKey(), round2(entry.getValue())));

        double refundPct = metrics.getOrdersCreated() == 0
                ? 0.0
                : (metrics.getRefundsCreated() * 100.0) / metrics.getOrdersCreated();
        log.info("Refund ratio: {}%", round2(refundPct));
        log.info("=================================================");
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
