package com.zosh.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryDataRepairComponent implements CommandLineRunner {

    private final InventoryConsistencyService inventoryConsistencyService;

    @Override
    public void run(String... args) {
        int repairedGroups = inventoryConsistencyService.repairDuplicateInventories();
        if (repairedGroups > 0) {
            log.warn("Merged {} duplicate inventory group(s) during startup repair.", repairedGroups);
        }
    }
}
