package org.example.orderservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryFailedEvent;
import org.example.commons.event.contracts.InventoryReservedEvent;
import org.example.orderservice.service.OrderService;
import org.example.orderservice.utils.Constants;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka listener responsible for handling incoming events
 * from other services
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
public class KafkaListenerService {

    private final OrderService orderService;

    /**
     * Handles successful inventory reservation.
     *
     * @param event inventory reserved event
     */
    @KafkaListener(
            topics = EventConstants.TOPIC_ORDER_INVENTORY_RESPONSE_V1,
            groupId = Constants.KAFKA_ORDER_GROUP_ID
    )
    public void handleInventoryReserved(InventoryReservedEvent event) {

        log.info("[ORDER-SERVICE][KAFKA] Inventory RESERVED for order {} correlationId {}",
                event.orderId(), event.correlationId());

        orderService.handleInventoryReserved(event);
    }

    /**
     * Handles failed inventory reservation.
     *
     * @param event inventory failed event
     */
    @KafkaListener(
            topics = EventConstants.TOPIC_ORDER_INVENTORY_RESPONSE_V1,
            groupId = Constants.KAFKA_ORDER_GROUP_ID
    )
    public void handleInventoryFailed(InventoryFailedEvent event) {

        log.warn("[ORDER-SERVICE][KAFKA] Inventory FAILED for order {} reason {} correlationId {}",
                event.orderId(), event.reason(), event.correlationId());

        orderService.handleInventoryFailed(event);
    }

}
