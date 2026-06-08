package org.example.orderservice.listener.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryFailedEvent;
import org.example.commons.event.contracts.InventoryReservedEvent;
import org.example.orderservice.service.OrderService;
import org.example.orderservice.utils.Constants;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka listener responsible for handling incoming events
 * from the inventory services
 *
 * <p>
 * This component represents the entry point for orchestration logic.
 * It receives results from external services and delegates processing
 * to the OrderService.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@KafkaListener(
        topics = EventConstants.TOPIC_ORDER_INVENTORY_RESPONSE_V1,
        groupId = Constants.KAFKA_ORDER_GROUP_ID
)
public class InventoryResponseKafkaListener {

    private final OrderService orderService;

    /**
     * Handles successfully event response from Inventory Service
     *
     * @param reserved Event indicates successfully item reservation
     */
    @KafkaHandler
    public void handleInventoryReserved(InventoryReservedEvent reserved) {
        log.info("[ORDER-SERVICE][KAFKA] Received InventoryReservedEvent for order {} correlationId {}",
                reserved.orderId(), reserved.correlationId());

        orderService.handleInventoryReserved(reserved);
    }

    /**
     * Handles failed event response from Inventory Service
     *
     * @param failed Event indicates failed item reservation
     */
    @KafkaHandler
    public void handleInventoryFailed(InventoryFailedEvent failed) {
        log.info("[ORDER-SERVICE][KAFKA] Received InventoryFailedEvent for order {}. Reason: {}",
                failed.orderId(), failed.reason());

        orderService.handleInventoryFailed(failed);
    }

    /**
     * Catch-all fallback method to capture unmapped data shapes safely without breaking consumers
     *
     * @param unknownMessage Unknown message
     */
    @KafkaHandler(isDefault = true)
    public void handleUnknownObject(Object unknownMessage) {
        log.error("[ORDER-SERVICE][KAFKA] Unmatched event signature fallback triggered! Payload object type: {}",
                unknownMessage.getClass().getName());
    }
}
