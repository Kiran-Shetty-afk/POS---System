package com.zosh.seed;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class SeedMetrics {
    private int storesCreated;
    private int branchesCreated;
    private int categoriesCreated;
    private int productsCreated;
    private int customersCreated;
    private int cashiersCreated;
    private int inventoriesCreated;
    private int ordersCreated;
    private int refundsCreated;
    private int shiftReportsCreated;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private final Map<String, Integer> ordersByBranch = new HashMap<>();
    private final Map<String, Double> salesByBranch = new HashMap<>();
}
