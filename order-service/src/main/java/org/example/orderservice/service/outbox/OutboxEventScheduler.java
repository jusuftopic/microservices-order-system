package org.example.orderservice.service.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.outbox.service.OutboxEventPublisherService;
import org.example.orderservice.lifecycle.ShutdownState;
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
    private final ShutdownState shutdownState;

    /**
     * Runs every 3 second to publish unprocessed outbox events.
     */
    @Scheduled(
            fixedDelayString = "${outbox.publisher.fixed-delay:3000}"
    )
    public void publishOutboxEvents() {
        if (shutdownState.isShuttingDown()) {
            log.debug("[ORDER-SERVICE][OUTBOX-EVENT-SCHEDULER] Skipping Outbox publication because shutdown is in progress");
            return;
        }

        publisherService.publishPendingEvents();
    }
}
