package org.example.orderservice.listener.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.events.InventoryCommitCompletedEvent;
import org.example.messagingstarter.contracts.events.InventoryCommitFailedEvent;
import org.example.messagingstarter.contracts.events.InventoryReleaseCompletedEvent;
import org.example.orderservice.service.OrderService;
import org.example.orderservice.utils.Constants;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka listener responsible for handling incoming events
 * from the inventory service related to inventory finalization.
 *
 * <p>
 * This component represents the entry point for inventory finalization
 * orchestration flow in the Order Service.
 * It receives results from inventory commit and release operations
 * and (later) will delegate further business handling to the OrderService.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@KafkaListener(
        topics = EventConstants.TOPIC_ORDER_INVENTORY_FINALIZATION_RESPONSE_V1,
        groupId = Constants.KAFKA_ORDER_GROUP_ID
)
public class InventoryFinalizationResponseKafkaListener {

    private final OrderService orderService;

    /**
     * Handles successful inventory commit event.
     *
     * @param event event indicating successful inventory finalization
     */
    @KafkaHandler
    public void handleInventoryCommitCompleted(
            InventoryCommitCompletedEvent event
    ) {

        log.info(
                "[ORDER-SERVICE][KAFKA] Received InventoryCommitCompletedEvent for order {} correlationId {}",
                event.orderId(),
                event.correlationId()
        );

        orderService.handleInventoryCommitCompleted(event);
    }

    /**
     * Handles failed inventory commit event.
     *
     * @param event event indicating failed inventory finalization
     */
    @KafkaHandler
    public void handleInventoryCommitFailed(
            InventoryCommitFailedEvent event
    ) {

        log.warn(
                "[ORDER-SERVICE][KAFKA] Received InventoryCommitFailedEvent for order {} reason {} correlationId {}",
                event.orderId(),
                event.reason(),
                event.correlationId()
        );

        orderService.handleInventoryCommitFailed(event);
    }

    /**
     * Handles successful inventory release event.
     *
     * @param event event indicating successful inventory release
     */
    @KafkaHandler
    public void handleInventoryReleaseCompleted(
            InventoryReleaseCompletedEvent event
    ) {

        log.info(
                "[ORDER-SERVICE][KAFKA] Received InventoryReleaseCompletedEvent for order {} correlationId {}",
                event.orderId(),
                event.correlationId()
        );

        orderService.handleInventoryReleaseCompleted(event);
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
                "[ORDER-SERVICE][KAFKA] Unmatched inventory finalization event signature fallback triggered! Payload object type: {}",
                unknownMessage.getClass().getName()
        );
    }
}
