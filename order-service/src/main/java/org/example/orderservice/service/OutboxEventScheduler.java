package org.example.orderservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduler responsible only for triggering outbox publishing.
 *
 * <p>
 * This class contains no business logic and delegates all work
 * to {@link OutboxEventPublisherService}.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class OutboxEventScheduler {

    private final OutboxEventPublisherService publisherService;

    /**
     * Runs every 3 second to publish unprocessed outbox events.
     */
    @Scheduled(fixedDelay = 3000)
    public void publishOutboxEvents() {
        publisherService.publishPendingEvents();
    }
}
