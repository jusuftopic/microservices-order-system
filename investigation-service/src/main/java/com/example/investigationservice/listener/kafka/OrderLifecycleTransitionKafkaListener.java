package com.example.investigationservice.listener.kafka;

import com.example.investigationservice.exception.InvalidLifecycleEventException;
import com.example.investigationservice.service.OrderLifecycleEvidencePersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.events.OrderLifecycleTransitionedEvent;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

/**
 * Receives authoritative order lifecycle facts for the investigation timeline.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@KafkaListener(
        topics = EventConstants.TOPIC_ORDER_LIFECYCLE_V1,
        groupId = "${spring.kafka.consumer.group-id}"
)
public class OrderLifecycleTransitionKafkaListener {

    private final OrderLifecycleEvidencePersistenceService persistenceService;

    /**
     * Handles an order lifecycle transition emitted by the Order Service.
     *
     * @param event lifecycle transition emitted by the Order Service
     * @param kafkaTopic source Kafka topic
     * @param kafkaPartition source Kafka partition
     * @param kafkaOffset source Kafka offset
     */
    @KafkaHandler
    public void handleOrderLifecycleTransitioned(
            OrderLifecycleTransitionedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String kafkaTopic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int kafkaPartition,
            @Header(KafkaHeaders.OFFSET) long kafkaOffset
    ) {
        boolean persisted = persistenceService.persist(event, kafkaTopic, kafkaPartition, kafkaOffset);

        log.info(
                "[INVESTIGATION-SERVICE][KAFKA] Processed OrderLifecycleTransitionedEvent "
                        + "for order {} transition {} -> {} reason {} correlationId {} messageId {} persisted {}",
                event.orderId(),
                event.previousStatus(),
                event.newStatus(),
                event.reasonCode(),
                event.correlationId(),
                event.messageId(),
                persisted
        );
    }

    /**
     * Rejects payloads that do not match the lifecycle event contract so the
     * configured error handler can preserve them for investigation.
     *
     * @param unknownMessage payload not mapped to a known lifecycle contract
     */
    @KafkaHandler(isDefault = true)
    public void handleUnknownObject(Object unknownMessage) {
        String payloadType = unknownMessage == null
                ? "null"
                : unknownMessage.getClass().getName();

        throw new InvalidLifecycleEventException(
                "Unmatched lifecycle event payload type: " + payloadType
        );
    }
}
