package com.zosh.service.impl;

import com.zosh.domain.UserRole;
import com.zosh.modal.Order;
import com.zosh.payload.StoreAnalysis.*;
import com.zosh.repository.*;
import com.zosh.service.StoreAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreAnalyticsServiceImpl implements StoreAnalyticsService {

    private final BranchRepository branchRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final RefundRepository refundRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    @Override
    public StoreOverviewDTO getStoreOverview(Long storeAdminId) {
        List<UserRole> roles = new ArrayList<>();
        roles.add(UserRole.ROLE_STORE_MANAGER);
        roles.add(UserRole.ROLE_CUSTOMER);
        roles.add(UserRole.ROLE_BRANCH_CASHIER);
        roles.add(UserRole.ROLE_BRANCH_MANAGER);

        return StoreOverviewDTO.builder()
                .totalBranches(branchRepository.countByStoreAdminId(storeAdminId))
                .totalSales(orderRepository.sumTotalSalesByStoreAdmin(storeAdminId).orElse(Double.valueOf(0)))
                .totalOrders(orderRepository.countByStoreAdminId(storeAdminId))
                .totalEmployees(userRepository.countByStoreAdminIdAndRoles(storeAdminId,roles))
                .totalCustomers(customerRepository.countByStoreAdminId(storeAdminId))
                .totalRefunds(refundRepository.countByStoreAdminId(storeAdminId))
                .totalProducts(productRepository.countByStoreAdminId(storeAdminId))
//                .topBranchName(branchRepository.findTopBranchBySales(storeAdminId))
                .build();
    }

    @Override
    public TimeSeriesDataDTO getSalesTrends(Long storeAdminId, String period) {
        String normalizedPeriod = period == null ? "daily" : period.toLowerCase(Locale.ROOT);
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start;

        switch (normalizedPeriod) {
            case "monthly" -> start = end.minusDays(365);
            case "weekly" -> start = end.minusDays(84);
            default -> start = end.minusDays(30);
        }

        List<Order> orders = orderRepository.findAllByStoreAdminAndCreatedAtBetween(storeAdminId, start, end);
        List<TimeSeriesPointDTO> points;

        if ("monthly".equals(normalizedPeriod)) {
            Map<YearMonth, Double> grouped = orders.stream()
                    .collect(Collectors.groupingBy(
                            order -> YearMonth.from(order.getCreatedAt()),
                            Collectors.summingDouble(order ->
                                    order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0
                            )
                    ));

            points = grouped.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new TimeSeriesPointDTO(
                            entry.getKey().atDay(1).atStartOfDay(),
                            entry.getValue()
                    ))
                    .collect(Collectors.toList());
        } else if ("weekly".equals(normalizedPeriod)) {
            WeekFields weekFields = WeekFields.ISO;
            Map<String, Double> grouped = orders.stream()
                    .collect(Collectors.groupingBy(
                            order -> {
                                int week = order.getCreatedAt().get(weekFields.weekOfWeekBasedYear());
                                int year = order.getCreatedAt().get(weekFields.weekBasedYear());
                                return year + "-" + week;
                            },
                            Collectors.summingDouble(order ->
                                    order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0
                            )
                    ));

            points = grouped.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .map(entry -> {
                        String[] parts = entry.getKey().split("-");
                        int year = Integer.parseInt(parts[0]);
                        int week = Integer.parseInt(parts[1]);
                        LocalDateTime weekStart = LocalDateTime.now()
                                .withYear(year)
                                .with(weekFields.weekOfWeekBasedYear(), week)
                                .with(weekFields.dayOfWeek(), 1)
                                .toLocalDate()
                                .atStartOfDay();
                        return new TimeSeriesPointDTO(weekStart, entry.getValue());
                    })
                    .collect(Collectors.toList());
        } else {
            Map<LocalDateTime, Double> grouped = orders.stream()
                    .collect(Collectors.groupingBy(
                            order -> order.getCreatedAt().toLocalDate().atStartOfDay(),
                            Collectors.summingDouble(order ->
                                    order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0
                            )
                    ));

            points = grouped.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new TimeSeriesPointDTO(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
        }

        return TimeSeriesDataDTO.builder()
                .period(normalizedPeriod.toUpperCase(Locale.ROOT))
                .points(points)
                .build();
    }

    @Override
    public List<TimeSeriesPointDTO> getMonthlySalesGraph(Long storeAdminId) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(365);

        List<Order> orders = orderRepository.findAllByStoreAdminAndCreatedAtBetween(storeAdminId, start, end);

        Map<YearMonth, Double> grouped = orders.stream()
                .collect(Collectors.groupingBy(
                        order -> YearMonth.from(order.getCreatedAt()),  // Group by Year-Month
                        Collectors.summingDouble(order ->
                                order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0
                        )
                ));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new TimeSeriesPointDTO(
                        entry.getKey().atDay(1).atStartOfDay(), // Convert YearMonth to LocalDateTime
                        entry.getValue()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<TimeSeriesPointDTO> getDailySalesGraph(Long storeAdminId) {
//        return null;
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(6);
        return orderRepository.getDailySales(storeAdminId, start, end);
    }

    @Override
    public List<CategorySalesDTO> getSalesByCategory(Long storeAdminId) {
        return productRepository.getSalesGroupedByCategory(storeAdminId);
    }

    @Override
    public List<PaymentInsightDTO> getSalesByPaymentMethod(Long storeAdminId) {
        return orderRepository.getSalesByPaymentMethod(storeAdminId);
    }

    @Override
    public List<BranchSalesDTO> getSalesByBranch(Long storeAdminId) {
        return orderRepository.getSalesByBranch(storeAdminId);
    }

    @Override
    public List<PaymentInsightDTO> getPaymentBreakdown(Long storeAdminId) {
        return orderRepository.getSalesByPaymentMethod(storeAdminId);
    }

    @Override
    public BranchPerformanceDTO getBranchPerformance(Long storeAdminId) {
        return BranchPerformanceDTO.builder()
                .branchSales(orderRepository.getSalesByBranch(storeAdminId))
                .newBranchesThisMonth(branchRepository.countNewBranchesThisMonth(storeAdminId))
//                .topBranch(branchRepository.findTopBranchBySales(storeAdminId))
                .build();
    }

    @Override
    public StoreAlertDTO getStoreAlerts(Long storeAdminId) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        userRepository.findInactiveCashiers(storeAdminId, sevenDaysAgo);

        return StoreAlertDTO.builder()
                .lowStockAlerts(productRepository.findLowStockProducts(storeAdminId))
                .noSalesToday(branchRepository.findBranchesWithNoSalesToday(storeAdminId))
                .refundSpikeAlerts(refundRepository.findRefundSpikes(storeAdminId))
                .inactiveCashiers(userRepository.findInactiveCashiers(storeAdminId, sevenDaysAgo))
                .build();
    }
}
