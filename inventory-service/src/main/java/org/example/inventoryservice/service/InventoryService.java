package org.example.inventoryservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryCheckRequestedEvent;
import org.example.commons.event.contracts.InventoryFailedEvent;
import org.example.commons.event.contracts.InventoryReservedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer responsible for handling business logic related to Order Inventory.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void processInventory(InventoryCheckRequestedEvent event) {
        boolean inventoryAvailable = true;

        if (inventoryAvailable) {
            publishSuccess(event);
        } else {
            publishFailure(event);
        }
    }


    private void publishSuccess(InventoryCheckRequestedEvent event) {

        InventoryReservedEvent response = new InventoryReservedEvent(
                event.orderId(),
                event.correlationId()
        );

        kafkaTemplate.send(
                EventConstants.TOPIC_ORDER_INVENTORY_RESPONSE_V1,
                event.orderId().toString(),
                response
        );

        log.info("[INVENTORY-SERVICE] Inventory reserved for order {}", event.orderId());
    }

    private void publishFailure(InventoryCheckRequestedEvent event) {

        InventoryFailedEvent response = new InventoryFailedEvent(
                event.orderId(),
                "OUT_OF_STOCK",
                event.correlationId()
        );

        kafkaTemplate.send(
                EventConstants.TOPIC_ORDER_INVENTORY_RESPONSE_V1,
                event.orderId().toString(),
                response
        );

        log.warn("[INVENTORY-SERVICE] Inventory FAILED for order {}", event.orderId());
    }

}
