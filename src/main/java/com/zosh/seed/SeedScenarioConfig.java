package com.zosh.seed;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeedScenarioConfig {
    private final SeedVolume volume;
    private final int days;
    private final boolean reset;
    private final long randomSeed;
    private final boolean includeShiftReports;
    private final boolean enabled;

    private final int storesCount;
    private final int branchesCount;
    private final int productsPerStore;
    private final int customersCount;
    private final int cashiersPerBranch;
    private final int targetOrders;
    private final double refundRate;
    private final String scenarioKey;

    public static SeedScenarioConfig of(SeedVolume volume,
                                        int days,
                                        boolean reset,
                                        long randomSeed,
                                        boolean includeShiftReports,
                                        boolean enabled) {
        Preset preset = Preset.forVolume(volume);
        return SeedScenarioConfig.builder()
                .volume(volume)
                .days(days)
                .reset(reset)
                .randomSeed(randomSeed)
                .includeShiftReports(includeShiftReports)
                .enabled(enabled)
                .storesCount(preset.storesCount)
                .branchesCount(preset.branchesCount)
                .productsPerStore(preset.productsPerStore)
                .customersCount(preset.customersCount)
                .cashiersPerBranch(preset.cashiersPerBranch)
                .targetOrders(preset.targetOrders)
                .refundRate(preset.refundRate)
                .scenarioKey("historical:" + volume.name().toLowerCase() + ":" + days + ":" + randomSeed)
                .build();
    }

    private record Preset(int storesCount,
                          int branchesCount,
                          int productsPerStore,
                          int customersCount,
                          int cashiersPerBranch,
                          int targetOrders,
                          double refundRate) {
        private static Preset forVolume(SeedVolume volume) {
            return switch (volume) {
                case SMALL -> new Preset(1, 3, 60, 120, 3, 1200, 0.02d);
                case MEDIUM -> new Preset(2, 5, 120, 300, 4, 4000, 0.03d);
                case LARGE -> new Preset(3, 8, 250, 700, 5, 10000, 0.035d);
            };
        }
    }
}
