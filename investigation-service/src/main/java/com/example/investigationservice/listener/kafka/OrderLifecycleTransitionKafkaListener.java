package com.example.investigationservice.listener.kafka;

import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.events.OrderLifecycleTransitionedEvent;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Receives authoritative order lifecycle facts for the investigation timeline.
 */
@Service
@Slf4j
@KafkaListener(
        topics = EventConstants.TOPIC_ORDER_LIFECYCLE_V1,
        groupId = "${spring.kafka.consumer.group-id}"
)
public class OrderLifecycleTransitionKafkaListener {

    /**
     * Handles an order lifecycle transition emitted by the Order Service.
     *
     * @param event lifecycle transition emitted by the Order Service
     */
    @KafkaHandler
    public void handleOrderLifecycleTransitioned(OrderLifecycleTransitionedEvent event) {
        log.info(
                "[INVESTIGATION-SERVICE][KAFKA] Received OrderLifecycleTransitionedEvent "
                        + "for order {} transition {} -> {} reason {} correlationId {} messageId {}",
                event.orderId(),
                event.previousStatus(),
                event.newStatus(),
                event.reasonCode(),
                event.correlationId(),
                event.messageId()
        );
    }

    /**
     * Handles an unexpected payload type without interrupting the consumer.
     *
     * @param unknownMessage payload not mapped to a known lifecycle contract
     */
    @KafkaHandler(isDefault = true)
    public void handleUnknownObject(Object unknownMessage) {
        log.error(
                "[INVESTIGATION-SERVICE][KAFKA] Unmatched lifecycle event signature. Payload object type: {}",
                unknownMessage.getClass().getName()
        );
    }
}
