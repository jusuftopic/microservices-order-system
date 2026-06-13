package org.example.inventoryservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryReserveRequestedEvent;
import org.example.inventoryservice.service.InventoryService;
import org.example.inventoryservice.utils.Constants;
import org.springframework.stereotype.Service;

/**
 * Kafka listener responsible for handling inventory-related events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaListener {

    private final InventoryService inventoryService;

    /**
     * Handles incoming inventory check requests.
     *
     * @param event Inventory check request event
     */
    @org.springframework.kafka.annotation.KafkaListener(topics = EventConstants.TOPIC_ORDER_INVENTORY_REQUEST_V1,
            groupId = Constants.KAFKA_INVENTORY_GROUP_ID
    )
    public void handleInventoryCheckRequested(InventoryReserveRequestedEvent event) {
        log.info("[INVENTORY-SERVICE][KAFKA] Received inventory request for order {} with {} items. CorrelationId {}",
                event.orderId(),
                event.items().size(),
                event.correlationId()
        );

        inventoryService.processInventory(event);
    }

    @org.springframework.kafka.annotation.KafkaListener(topics = EventConstants.TOPIC_INVENTORY_DLQ,
            groupId = Constants.KAFKA_INVENTORY_GROUP_ID)
    public void handlePaymentRequestedDLT(String message) {
        log.warn("[INVENTORY-SERVICE][KAFKA-LISTENER] Received DLT message for event {}", EventConstants.EVENT_INVENTORY_CHECK_REQUESTED);
    }
}
