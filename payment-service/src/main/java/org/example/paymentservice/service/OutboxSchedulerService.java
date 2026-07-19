package org.example.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.outbox.service.OutboxEventPublisherService;
import org.example.paymentservice.lifecycle.ShutdownState;
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
            log.debug("[PAYMENT-SERVICE][OUTBOX-EVENT-SCHEDULER] Skipping Outbox publication because shutdown is in progress");
            return;
        }

        publisher.publishPendingEvents();
    }
}
