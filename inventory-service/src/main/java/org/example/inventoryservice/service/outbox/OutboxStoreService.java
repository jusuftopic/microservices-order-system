package org.example.inventoryservice.service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.outbox.entity.OutboxEvent;
import org.example.messagingstarter.outbox.repository.OutboxRepository;
import org.example.messagingstarter.outbox.service.OutboxDlqService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service responsible for storing outbox events.
 *
 * <p>
 * This component encapsulates serialization and persistence
 * of outbox messages and handles DLQ fallback in case of failures.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxStoreService {

    private final OutboxRepository outboxRepository;
    private final OutboxDlqService outboxDlqService;
    private final ObjectMapper objectMapper;

    /**
     * Stores an event in the outbox table.
     *
     * @param payload event payload
     * @param eventType type of the event
     * @param aggregateId aggregate identifier
     */
    public void store(Object payload, String eventType, Long aggregateId) {

        try {
            OutboxEvent event = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateType("INVENTORY")
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .processed(false)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxRepository.save(event);

            log.info(
                    "[INVENTORY-SERVICE][OUTBOX] Stored event {} ({})",
                    event.getId(),
                    eventType
            );

        } catch (Exception e) {

            log.error(
                    "[INVENTORY-SERVICE][OUTBOX] Failed to serialize payload for {} (aggregateId={}). Moved to DLQ.",
                    eventType,
                    aggregateId,
                    e
            );

            outboxDlqService.storeOutboxDlq(
                    null,
                    aggregateId,
                    eventType,
                    payload,
                    0,
                    e
            );
        }
    }

}
