package org.example.orderservice.service.outbox;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.outbox.entity.OutboxEvent;
import org.example.messagingstarter.outbox.repository.OutboxRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderOutboxService {

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;


    /**
     * Stores outbox event for asynchronous Kafka publishing.
     *
     * @param aggregateId aggregate identifier
     * @param aggregateType aggregate type
     * @param eventType event type
     * @param payload event payload
     */
    public void storeEvent(
            Long aggregateId,
            String aggregateType,
            String eventType,
            Object payload
    ) {

        final OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setId(UUID.randomUUID());
        outboxEvent.setAggregateType(aggregateType);
        outboxEvent.setAggregateId(aggregateId);
        outboxEvent.setEventType(eventType);
        outboxEvent.setPayload(toJson(payload));
        outboxEvent.setProcessed(false);
        outboxEvent.setCreatedAt(LocalDateTime.now());

        repository.save(outboxEvent);

        log.info(
                "[ORDER-SERVICE][OUTBOX] Stored event {} aggregateId {}",
                eventType,
                aggregateId
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        }
        catch (JsonProcessingException ex) {
            log.error(
                    "[ORDER-SERVICE][OUTBOX] Failed to serialize payload.",
                    ex
            );

            throw new IllegalStateException(
                    "Failed to serialize outbox payload",
                    ex
            );
        }
    }

}
