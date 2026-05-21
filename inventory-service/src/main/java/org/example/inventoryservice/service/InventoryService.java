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
 * Service responsible for inventory processing logic.
 *
 * <p>
 * This service handles validation and reservation of inventory items.
 * It is triggered by Kafka events and produces outcome events.
 * </p>
 *
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryService {

    private final KafkaPublisherService kafkaPublisherService;


    /**
     * Processes inventory check request.
     *
     * <p>
     * This method evaluates inventory availability and emits a result event.
     * </p>
     *
     * @param event inventory check request event
     */
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

        kafkaPublisherService.publishInventoryReserved(response);

        log.info("[INVENTORY-SERVICE] Inventory reserved for order {}", event.orderId());
    }

    private void publishFailure(InventoryCheckRequestedEvent event) {

        InventoryFailedEvent response = new InventoryFailedEvent(
                event.orderId(),
                "OUT_OF_STOCK",
                event.correlationId()
        );

        kafkaPublisherService.publishInventoryFailed(response);

        log.warn("[INVENTORY-SERVICE] Inventory FAILED for order {}", event.orderId());
    }

}
