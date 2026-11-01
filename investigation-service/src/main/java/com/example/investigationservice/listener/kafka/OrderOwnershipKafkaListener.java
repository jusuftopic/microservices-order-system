package com.example.investigationservice.listener.kafka;

import com.example.investigationservice.exception.InvalidOrderOwnershipEventException;
import com.example.investigationservice.service.OrderOwnershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.events.OrderOwnershipAssignedEvent;
import org.example.messagingstarter.kafka.KafkaConsumerReliabilitySupport;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

/**
 * Receives authoritative order ownership facts for the local access projection.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@KafkaListener(
        topics = EventConstants.TOPIC_ORDER_OWNERSHIP_V1,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = KafkaConsumerReliabilitySupport.BUSINESS_LISTENER_FACTORY
)
public class OrderOwnershipKafkaListener {

    private final OrderOwnershipService ownershipService;

    /**
     * Persists an ownership assignment received from the Order Service.
     *
     * @param event ownership event
     * @param kafkaTopic source Kafka topic
     * @param kafkaPartition source Kafka partition
     * @param kafkaOffset source Kafka offset
     */
    @KafkaHandler
    public void handleOrderOwnershipAssigned(
            OrderOwnershipAssignedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String kafkaTopic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int kafkaPartition,
            @Header(KafkaHeaders.OFFSET) long kafkaOffset
    ) {
        boolean persisted = ownershipService.persist(event, kafkaTopic, kafkaPartition, kafkaOffset);

        log.info(
                "[INVESTIGATION-SERVICE][KAFKA] Processed "
                        + "OrderOwnershipAssignedEvent for order {} correlationId {} "
                        + "messageId {} persisted {}",
                event.orderId(), event.correlationId(),
                event.messageId(), persisted
        );
    }

    /**
     * Rejects payloads that do not match the ownership contract.
     *
     * @param unknownMessage payload not mapped to the ownership event type
     */
    @KafkaHandler(isDefault = true)
    public void handleUnknownObject(Object unknownMessage) {
        String payloadType = unknownMessage == null
                ? "null"
                : unknownMessage.getClass().getName();
        throw new InvalidOrderOwnershipEventException(
                "Unmatched order ownership payload type: " + payloadType
        );
    }
}
