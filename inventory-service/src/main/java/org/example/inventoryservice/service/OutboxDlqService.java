package org.example.inventoryservice.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.inventoryservice.entity.OutboxDlqEvent;
import org.example.inventoryservice.repository.OutboxDlqRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Centralized service responsible for handling DLQ persistence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxDlqService {

    private final OutboxDlqRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Stores failed event into DLQ.
     *
     * @param originalEventId original event ID (can be null)
     * @param aggregateId business aggregate id
     * @param eventType event type
     * @param payload payload object or raw JSON
     * @param retryCount retry attempts
     * @param exception failure reason
     */
    public void storeOutboxDlq(UUID originalEventId, Long aggregateId,
            String eventType, Object payload,
            int retryCount, Throwable exception
    ) {

        try {
            OutboxDlqEvent dlqEvent = new OutboxDlqEvent();
            dlqEvent.setOriginalEventId(originalEventId);
            dlqEvent.setAggregateId(aggregateId);
            dlqEvent.setEventType(eventType);
            dlqEvent.setPayload(safeToString(payload));
            dlqEvent.setErrorMessage(exception.getMessage());
            dlqEvent.setRetryCount(retryCount);

            repository.save(dlqEvent);

            log.warn("[INVENTORY-SERVICE][DLQ] Stored event type={} aggregateId={} retryCount={}",
                    eventType, aggregateId, retryCount, exception);

        } catch (Exception ex) {
            log.error("[INVENTORY-SERVICE][DLQ] CRITICAL: Failed to persist DLQ event. Payload LOST!", ex);
        }
    }

    private String safeToString(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return String.valueOf(payload);
        }
    }

}
