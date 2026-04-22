package com.zosh.seed;

import com.zosh.domain.PaymentType;

import java.util.List;
import java.util.Map;

public record BranchBehaviorProfile(
        String branchName,
        double baselineOrdersPerDay,
        double weekendMultiplier,
        double weekdayMultiplier,
        double monthlyTrendFactor,
        Map<PaymentType, Double> paymentMix,
        List<Integer> topProductIndexes
) {
}
