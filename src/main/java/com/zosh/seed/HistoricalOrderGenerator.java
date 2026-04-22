package com.zosh.seed;

import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentType;
import com.zosh.modal.*;
import com.zosh.repository.InventoryRepository;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.ShiftReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@RequiredArgsConstructor
public class HistoricalOrderGenerator {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final ShiftReportRepository shiftReportRepository;

    public List<Order> generate(SeedScenarioConfig config,
                                HistoricalSeedContext context,
                                Random random,
                                SeedMetrics metrics) {
        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate = endDate.minusDays(config.getDays() - 1L);
        metrics.setRangeStart(startDate.atStartOfDay());
        metrics.setRangeEnd(endDate.atTime(LocalTime.MAX));

        List<Customer> repeatCandidates = context.getCustomers()
                .subList(0, Math.max(1, context.getCustomers().size() / 4));

        List<Order> allOrders = new ArrayList<>();
        int createdOrders = 0;
        List<Branch> activeBranches = context.getBranches().stream()
                .filter(branch -> {
                    List<User> cashiers = context.getCashiersByBranchId().getOrDefault(branch.getId(), List.of());
                    List<Product> products = context.getProductsByStoreId().getOrDefault(branch.getStore().getId(), List.of());
                    return !cashiers.isEmpty() && !products.isEmpty();
                })
                .toList();

        for (LocalDate date = startDate; !date.isAfter(endDate) && createdOrders < config.getTargetOrders(); date = date.plusDays(1)) {
            int remainingOrders = config.getTargetOrders() - createdOrders;
            int remainingDays = Math.max(1, (int) ChronoUnit.DAYS.between(date, endDate) + 1);
            int dailyTarget = Math.max(1, (int) Math.round((double) remainingOrders / remainingDays));
            dailyTarget = Math.min(remainingOrders, applyDailyNoise(dailyTarget, random));

            for (int i = 0; i < dailyTarget && createdOrders < config.getTargetOrders(); i++) {
                Branch branch = pickBranchForDate(activeBranches, context, date, random);
                BranchBehaviorProfile profile = context.getBehaviorByBranchId().get(branch.getId());
                List<User> cashiers = context.getCashiersByBranchId().getOrDefault(branch.getId(), List.of());
                List<Product> branchProducts = context.getProductsByStoreId()
                        .getOrDefault(branch.getStore().getId(), List.of());

                User cashier = cashiers.get(random.nextInt(cashiers.size()));
                Customer customer = pickCustomer(context.getCustomers(), repeatCandidates, random);
                List<OrderItem> items = pickItems(branch, branchProducts, profile, random);
                if (items.isEmpty()) {
                    continue;
                }

                LocalDateTime createdAt = date.atTime(9 + random.nextInt(12), random.nextInt(60));
                PaymentType paymentType = pickPaymentType(profile, random);
                Order order = new Order();
                order.setBranch(branch);
                order.setCashier(cashier);
                order.setCustomer(customer);
                order.setCreatedAt(createdAt);
                order.setPaymentType(paymentType);
                order.setStatus(OrderStatus.COMPLETED);

                double totalAmount = items.stream().mapToDouble(OrderItem::getPrice).sum();
                order.setTotalAmount(round2(totalAmount));
                items.forEach(orderItem -> orderItem.setOrder(order));
                order.setItems(items);

                Order saved = orderRepository.save(order);
                allOrders.add(saved);
                createdOrders++;
                metrics.getOrdersByBranch().merge(branch.getName(), 1, Integer::sum);
                metrics.getSalesByBranch().merge(branch.getName(), saved.getTotalAmount(), Double::sum);
            }
        }
        metrics.setOrdersCreated(allOrders.size());

        if (config.isIncludeShiftReports()) {
            createShiftReports(context, startDate, endDate, random, metrics);
        }
        return allOrders;
    }

    private int computeOrdersForDay(LocalDate date, BranchBehaviorProfile profile, Random random) {
        double monthTrend = 1.0 + ((date.getMonthValue() % 4) * 0.05);
        double seasonalSpike = (date.getDayOfMonth() >= 10 && date.getDayOfMonth() <= 14 && date.getMonthValue() % 2 == 0)
                ? 1.45
                : 1.0;
        boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        double dayFactor = weekend ? profile.weekendMultiplier() : profile.weekdayMultiplier();
        double expected = profile.baselineOrdersPerDay()
                * dayFactor
                * profile.monthlyTrendFactor()
                * monthTrend
                * seasonalSpike;
        int noise = random.nextInt(5) - 2;
        return Math.max(3, (int) Math.round(expected) + noise);
    }

    private int applyDailyNoise(int dailyTarget, Random random) {
        int variance = Math.max(1, (int) Math.round(dailyTarget * 0.2));
        int noise = random.nextInt((variance * 2) + 1) - variance;
        return Math.max(1, dailyTarget + noise);
    }

    private Branch pickBranchForDate(List<Branch> activeBranches,
                                     HistoricalSeedContext context,
                                     LocalDate date,
                                     Random random) {
        if (activeBranches.size() == 1) {
            return activeBranches.get(0);
        }
        double totalWeight = 0.0;
        List<Double> weights = new ArrayList<>(activeBranches.size());
        for (Branch branch : activeBranches) {
            BranchBehaviorProfile profile = context.getBehaviorByBranchId().get(branch.getId());
            double weight = Math.max(1.0, computeOrdersForDay(date, profile, random));
            weights.add(weight);
            totalWeight += weight;
        }

        double threshold = random.nextDouble() * totalWeight;
        double cumulative = 0.0;
        for (int i = 0; i < activeBranches.size(); i++) {
            cumulative += weights.get(i);
            if (threshold <= cumulative) {
                return activeBranches.get(i);
            }
        }
        return activeBranches.get(activeBranches.size() - 1);
    }

    private Customer pickCustomer(List<Customer> customers, List<Customer> repeatCandidates, Random random) {
        if (customers.isEmpty()) {
            return null;
        }
        // 65% of orders come from repeat customers to create a realistic retention pattern.
        boolean repeat = random.nextDouble() < 0.65;
        List<Customer> source = repeat ? repeatCandidates : customers;
        return source.get(random.nextInt(source.size()));
    }

    private List<OrderItem> pickItems(Branch branch,
                                      List<Product> branchProducts,
                                      BranchBehaviorProfile profile,
                                      Random random) {
        int itemCount = 1 + random.nextInt(4);
        List<OrderItem> items = new ArrayList<>();
        Set<Long> pickedProductIds = new HashSet<>();

        for (int i = 0; i < itemCount; i++) {
            Product product = pickProduct(branchProducts, profile, random);
            if (!pickedProductIds.add(product.getId())) {
                continue;
            }
            int qty = 1 + random.nextInt(3);
            List<Inventory> inventories = inventoryRepository.findAllByBranchIdAndProductIdOrderByIdAsc(branch.getId(), product.getId());
            if (inventories.isEmpty() || inventories.get(0).getQuantity() < qty) {
                continue;
            }
            Inventory inventory = inventories.get(0);
            inventory.setQuantity(Math.max(0, inventory.getQuantity() - qty));
            inventoryRepository.save(inventory);

            items.add(OrderItem.builder()
                    .product(product)
                    .quantity(qty)
                    .price(round2(product.getSellingPrice() * qty))
                    .build());
        }
        return items;
    }

    private Product pickProduct(List<Product> branchProducts, BranchBehaviorProfile profile, Random random) {
        if (branchProducts.isEmpty()) {
            throw new IllegalArgumentException("Branch products cannot be empty");
        }
        boolean useTop = random.nextDouble() < 0.55 && !profile.topProductIndexes().isEmpty();
        if (useTop) {
            int index = profile.topProductIndexes().get(random.nextInt(profile.topProductIndexes().size()));
            index = Math.floorMod(index, branchProducts.size());
            return branchProducts.get(index);
        }
        return branchProducts.get(random.nextInt(branchProducts.size()));
    }

    private PaymentType pickPaymentType(BranchBehaviorProfile profile, Random random) {
        double value = random.nextDouble();
        double cumulative = 0.0;
        for (Map.Entry<PaymentType, Double> entry : profile.paymentMix().entrySet()) {
            cumulative += entry.getValue();
            if (value <= cumulative) {
                return entry.getKey();
            }
        }
        return PaymentType.CASH;
    }

    private void createShiftReports(HistoricalSeedContext context,
                                    LocalDate startDate,
                                    LocalDate endDate,
                                    Random random,
                                    SeedMetrics metrics) {
        int created = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            for (Branch branch : context.getBranches()) {
                List<User> branchCashiers = context.getCashiersByBranchId().getOrDefault(branch.getId(), List.of());
                if (branchCashiers.isEmpty()) {
                    continue;
                }
                User cashier = branchCashiers.get(random.nextInt(branchCashiers.size()));
                ShiftReport report = new ShiftReport();
                report.setBranch(branch);
                report.setCashier(cashier);
                report.setShiftStart(date.atTime(9, 0));
                report.setShiftEnd(date.atTime(18, 0));
                report.setTotalOrders(0);
                report.setTotalSales(0.0);
                report.setTotalRefunds(0.0);
                report.setNetSales(0.0);
                shiftReportRepository.save(report);
                created++;
            }
        }
        metrics.setShiftReportsCreated(created);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
