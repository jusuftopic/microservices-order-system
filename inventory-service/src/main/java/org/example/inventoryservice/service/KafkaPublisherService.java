package org.example.inventoryservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryFailedEvent;
import org.example.commons.event.contracts.InventoryReservedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service responsible for publishing inventory-related events to Kafka.
 *
 * <p>
 * This class encapsulates all Kafka communication logic for the Inventory Service.
 * It ensures that topic usage is centralized and prevents scattering
 * Kafka-related code across business services.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Publish inventory success events</li>
 *     <li>Publish inventory failure events</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;


    /**
     * Publishes InventoryReservedEvent to Kafka.
     *
     * @param event event containing successful inventory reservation information
     */
    public void publishInventoryReserved(InventoryReservedEvent event) {
        log.info("[INVENTORY-SERVICE][KAFKA] Sending InventoryReservedEvent for order {} correlationId {}",
                event.orderId(), event.correlationId());
        log.info("KafkaTemplate class: {}", kafkaTemplate);

        kafkaTemplate.send(
                EventConstants.TOPIC_ORDER_INVENTORY_RESPONSE_V1,
                event.orderId().toString(),
                event
        );

        log.info("[INVENTORY-SERVICE][KAFKA] Published InventoryReservedEvent for order {} correlationId {}",
                event.orderId(), event.correlationId());
    }


    /**
     * Publishes InventoryFailedEvent to Kafka.
     *
     * @param event event containing failure reason for inventory reservation
     */
    public void publishInventoryFailed(InventoryFailedEvent event) {
        kafkaTemplate.send(
                EventConstants.TOPIC_ORDER_INVENTORY_RESPONSE_V1,
                event.orderId().toString(),
                event
        );

        log.warn("[INVENTORY-SERVICE][KAFKA] Published InventoryFailedEvent for order {} correlationId {} reason {}",
                event.orderId(), event.correlationId(), event.reason());
    }


}
