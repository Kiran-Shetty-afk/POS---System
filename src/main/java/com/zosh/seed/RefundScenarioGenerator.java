package com.zosh.seed;

import com.zosh.domain.OrderStatus;
import com.zosh.modal.Order;
import com.zosh.modal.Refund;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class RefundScenarioGenerator {

    private static final List<String> REASONS = List.of(
            "Customer changed mind",
            "Packaging issue",
            "Incorrect item billed",
            "Item quality concern",
            "Duplicate payment suspected"
    );

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;

    public List<Refund> generateRefunds(SeedScenarioConfig config,
                                        List<Order> orders,
                                        Random random,
                                        SeedMetrics metrics) {
        if (orders.isEmpty()) {
            return List.of();
        }
        int refundTarget = Math.max(1, (int) Math.floor(orders.size() * config.getRefundRate()));
        List<Order> shuffled = new ArrayList<>(orders);
        Collections.shuffle(shuffled, random);

        LocalDate anomalyStart = LocalDate.now().minusDays(10);
        LocalDate anomalyEnd = LocalDate.now().minusDays(4);

        List<Refund> refunds = new ArrayList<>();
        int created = 0;
        for (Order order : shuffled) {
            if (created >= refundTarget) {
                break;
            }
            Refund refund = new Refund();
            refund.setOrder(order);
            refund.setBranch(order.getBranch());
            refund.setCashier(order.getCashier());
            refund.setAmount(order.getTotalAmount());
            refund.setReason(REASONS.get(random.nextInt(REASONS.size())));
            refund.setPaymentType(order.getPaymentType());
            refund.setCreatedAt(refundTimestamp(order.getCreatedAt(), anomalyStart, anomalyEnd, random));
            refunds.add(refundRepository.save(refund));

            order.setStatus(OrderStatus.REFUNDED);
            orderRepository.save(order);
            created++;
        }
        metrics.setRefundsCreated(refunds.size());
        return refunds;
    }

    private LocalDateTime refundTimestamp(LocalDateTime orderCreatedAt,
                                          LocalDate anomalyStart,
                                          LocalDate anomalyEnd,
                                          Random random) {
        if (random.nextDouble() < 0.25) {
            LocalDate day = anomalyStart.plusDays(random.nextInt((int) (anomalyEnd.toEpochDay() - anomalyStart.toEpochDay() + 1)));
            return day.atTime(11 + random.nextInt(9), random.nextInt(60));
        }
        return orderCreatedAt.plusHours(2 + random.nextInt(72));
    }
}
