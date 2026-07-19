package org.example.inventoryservice.service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.inventoryservice.lifecycle.ShutdownState;
import org.example.messagingstarter.outbox.service.OutboxEventPublisherService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


/**
 * Periodically publishes pending outbox events.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxSchedulerService {

    private final OutboxEventPublisherService publisher;
    private final ShutdownState shutdownState;

    @Scheduled(fixedDelay = 3000)
    public void publish() {
        if (shutdownState.isShuttingDown()) {
            log.debug("[INVENTORY-SERVICE][OUTBOX-EVENT-SCHEDULER] Skipping Outbox publication because shutdown is in progress");
            return;
        }

        publisher.publishPendingEvents();
    }
}
