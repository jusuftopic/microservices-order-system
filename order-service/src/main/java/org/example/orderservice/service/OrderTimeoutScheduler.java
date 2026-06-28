package org.example.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.entity.Order;
import org.example.orderservice.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Periodically checks for stuck orders
 * and triggers timeout handling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutScheduler {

    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private final OrderRepository repository;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 6, timeUnit = TimeUnit.MINUTES)
    public void checkTimedOutOrders() {

        LocalDateTime threshold =
                LocalDateTime.now().minus(TIMEOUT);

        List<Order> timedOutOrders =
                repository.findTimedOutOrders(threshold);

        timedOutOrders.forEach(orderService::handleTimeout);

        if (!timedOutOrders.isEmpty()) {
            log.warn(
                    "[ORDER-TIMEOUT] Detected {} timed out orders",
                    timedOutOrders.size()
            );
        }
    }
}
