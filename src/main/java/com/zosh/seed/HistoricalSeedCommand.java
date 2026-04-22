package com.zosh.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class HistoricalSeedCommand implements CommandLineRunner {

    private final HistoricalSeedService historicalSeedService;

    @Override
    public void run(String... args) {
        ArgReader reader = new ArgReader(args);
        boolean enabled = reader.getBoolean("seed.historical", false);
        if (!enabled) {
            return;
        }

        SeedVolume volume = reader.getEnum("seed.volume", SeedVolume.class, SeedVolume.SMALL);
        int days = reader.getInt("seed.days", 90);
        boolean reset = reader.getBoolean("seed.reset", false);
        long randomSeed = reader.getLong("seed.random", 42L);
        boolean includeShiftReports = reader.getBoolean("seed.shifts", true);

        SeedScenarioConfig config = SeedScenarioConfig.of(
                volume,
                days,
                reset,
                randomSeed,
                includeShiftReports,
                true
        );
        log.info("Historical seeding requested. volume={}, days={}, reset={}, randomSeed={}",
                volume, days, reset, randomSeed);
        historicalSeedService.seedHistoricalData(config);
    }

    private static class ArgReader {
        private final String[] args;

        private ArgReader(String[] args) {
            this.args = args == null ? new String[0] : args;
        }

        private boolean getBoolean(String key, boolean defaultValue) {
            String value = getRaw(key);
            if (value == null) {
                return defaultValue;
            }
            return Boolean.parseBoolean(value);
        }

        private int getInt(String key, int defaultValue) {
            String value = getRaw(key);
            if (value == null) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        }

        private long getLong(String key, long defaultValue) {
            String value = getRaw(key);
            if (value == null) {
                return defaultValue;
            }
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        }

        private <E extends Enum<E>> E getEnum(String key, Class<E> enumType, E defaultValue) {
            String value = getRaw(key);
            if (value == null) {
                return defaultValue;
            }
            try {
                return Enum.valueOf(enumType, value.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return defaultValue;
            }
        }

        private String getRaw(String key) {
            String prefix = "--" + key + "=";
            return Arrays.stream(args)
                    .filter(arg -> arg != null && arg.startsWith(prefix))
                    .map(arg -> arg.substring(prefix.length()))
                    .findFirst()
                    .orElse(null);
        }
    }
}
