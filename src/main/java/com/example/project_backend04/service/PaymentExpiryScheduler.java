package com.example.project_backend04.service;

import com.example.project_backend04.entity.PaymentOrder;
import com.example.project_backend04.enums.PaymentStatus;
import com.example.project_backend04.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExpiryScheduler {

    private final PaymentOrderRepository paymentOrderRepository;

    @Scheduled(fixedDelay = 5 * 60 * 1000) // 5 phút
    @Transactional
    public void expireOverduePayments() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Running payment expiry check at {}", now);

        List<PaymentOrder> expiredOrders = paymentOrderRepository
                .findByStatusAndExpiredAtBefore(PaymentStatus.PENDING, now);

        if (expiredOrders.isEmpty()) {
            log.debug("No expired payments found");
            return;
        }

        log.info("Found {} expired PENDING payment(s), updating to FAILED", expiredOrders.size());

        for (PaymentOrder order : expiredOrders) {
            order.setStatus(PaymentStatus.FAILED);
            log.info("Expired payment order: id={}, content={}, expiredAt={}",
                    order.getId(), order.getContent(), order.getExpiredAt());
        }

        paymentOrderRepository.saveAll(expiredOrders);
        log.info("Payment expiry check completed. {} order(s) marked as FAILED", expiredOrders.size());
    }
}
