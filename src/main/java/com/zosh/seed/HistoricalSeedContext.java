package com.zosh.seed;

import com.zosh.modal.*;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class HistoricalSeedContext {
    private final List<Store> stores;
    private final List<Branch> branches;
    private final List<User> cashiers;
    private final List<Customer> customers;
    private final List<Product> products;
    private final List<Category> categories;
    private final Map<Long, List<Product>> productsByStoreId;
    private final Map<Long, List<User>> cashiersByBranchId;
    private final Map<Long, BranchBehaviorProfile> behaviorByBranchId;
}
