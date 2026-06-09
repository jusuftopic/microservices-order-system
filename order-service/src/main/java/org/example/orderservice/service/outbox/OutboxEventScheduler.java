package org.example.orderservice.service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.service.publisher.OutboxEventPublisherService;
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
@Slf4j
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
