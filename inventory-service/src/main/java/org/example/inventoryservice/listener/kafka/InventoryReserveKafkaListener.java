package org.example.inventoryservice.listener.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.InventoryReserveRequestedEvent;
import org.example.inventoryservice.service.InventoryService;
import org.example.inventoryservice.utils.Constants;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


/**
 * Kafka listener responsible for handling incoming inventory reservation requests.
 *
 * <p>
 * This component processes initial inventory reservation requests
 * coming from the Order Service and delegates execution to InventoryService.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@KafkaListener(
        topics = EventConstants.TOPIC_ORDER_INVENTORY_REQUEST_V1,
        groupId = Constants.KAFKA_INVENTORY_GROUP_ID
)
public class InventoryReserveKafkaListener {

    private final InventoryService inventoryService;

    /**
     * Handles inventory reservation request event.
     *
     * @param event event requesting inventory reservation
     */
    @KafkaHandler
    public void handleInventoryReserveRequested(
            InventoryReserveRequestedEvent event
    ) {

        log.info(
                "[INVENTORY-SERVICE][KAFKA] Received inventory reservation request for order {} with {} items. CorrelationId {}",
                event.orderId(),
                event.items().size(),
                event.correlationId()
        );

        inventoryService.processInventory(event);
    }


    /**
     * Catch-all fallback method to capture unmapped data shapes safely
     * without breaking consumers.
     *
     * @param unknownMessage unknown message
     */
    @KafkaHandler(isDefault = true)
    public void handleUnknownObject(Object unknownMessage) {

        log.error(
                "[INVENTORY-SERVICE][KAFKA] Unmatched reservation event signature fallback triggered! Payload object type: {}",
                unknownMessage.getClass().getName()
        );
    }



}
