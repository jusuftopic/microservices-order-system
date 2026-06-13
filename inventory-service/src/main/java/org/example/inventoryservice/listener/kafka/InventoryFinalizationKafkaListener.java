package org.example.inventoryservice.listener.kafka;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryCommitEvent;
import org.example.commons.event.contracts.InventoryReleasedRequestedEvent;
import org.example.inventoryservice.service.InventoryService;
import org.example.inventoryservice.utils.Constants;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka listener responsible for handling inventory finalization events.
 *
 * <p>
 * This listener processes post-payment inventory actions such as:
 * committing reserved inventory or releasing it for compensation.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@KafkaListener(
        topics = EventConstants.TOPIC_ORDER_INVENTORY_FINALIZATION_REQUEST_V1,
        groupId = Constants.KAFKA_INVENTORY_GROUP_ID
)
public class InventoryFinalizationKafkaListener {

    private final InventoryService inventoryService;

    /**
     * Handles inventory commit request.
     *
     * @param event event requesting inventory commitment
     */
    @KafkaHandler
    public void handleInventoryCommitRequested(
            InventoryCommitEvent event
    ) {

        log.info(
                "[INVENTORY-SERVICE][KAFKA] Received inventory commit request for order {} correlationId {}",
                event.orderId(),
                event.correlationId()
        );

        inventoryService.processCommit(event);
    }


    /**
     * Handles inventory release request (compensation flow).
     *
     * @param event event requesting inventory release
     */
    @KafkaHandler
    public void handleInventoryReleaseRequested(
            InventoryReleasedRequestedEvent event
    ) {

        log.info(
                "[INVENTORY-SERVICE][KAFKA] Received inventory release request for order {} correlationId {}",
                event.orderId(),
                event.correlationId()
        );

        inventoryService.processRelease(event);
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
                "[INVENTORY-SERVICE][KAFKA] Unmatched finalization event signature fallback triggered! Payload object type: {}",
                unknownMessage.getClass().getName()
        );
    }

}
