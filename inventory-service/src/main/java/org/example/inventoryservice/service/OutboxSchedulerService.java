package org.example.inventoryservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.inventoryservice.service.publisher.OutboxPublisherService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


/**
 * Periodically publishes pending outbox events.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxSchedulerService {

    private final OutboxPublisherService publisher;

    @Scheduled(fixedDelay = 3000)
    public void publish() {
        log.info("[INVENTORY-SERVICE] Schedule started");
        publisher.publishPendingEvents();
    }
}
