package org.example.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.PaymentRequestedEvent;
import org.example.orderservice.entity.OutboxEvent;
import org.example.orderservice.repository.OutboxRepository;
import org.example.orderservice.service.publisher.OutboxEventPublisherService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

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

    private final ObjectMapper objectMapper;
    private boolean isSet = false;
    private final OutboxRepository outboxRepository;

    /**
     * Runs every 3 second to publish unprocessed outbox events.
     */
    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void publishOutboxEvents() {
        if (!isSet) set();

        publisherService.publishPendingEvents();
    }

    private void set() {
        final OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateType("TEST");
        event.setAggregateId(1L);
        event.setEventType("TEST");
        try {
            event.setPayload(
                    objectMapper.writeValueAsString(
                            new PaymentRequestedEvent(
                                    UUID.randomUUID(),
                                    1L, BigDecimal.valueOf(1L), "test"
                            )
                    )
            );
        }
        catch (Exception e) {
            event.setPayload("");
        }

        event.setProcessed(true);
        event.setRetryCount(0);

        outboxRepository.save(event);
        isSet = true;
        log.info("[SCHEDULER] Stored");
    }
}
