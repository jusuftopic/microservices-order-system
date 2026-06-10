package org.example.inventoryservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryCheckRequestedEvent;
import org.example.commons.event.contracts.InventoryFailedEvent;
import org.example.commons.event.contracts.InventoryReservedEvent;
import org.example.commons.event.contracts.OrderItemEvent;
import org.example.inventoryservice.entity.InventoryItem;
import org.example.inventoryservice.entity.OutboxEvent;
import org.example.inventoryservice.repository.InboxRepository;
import org.example.inventoryservice.repository.InventoryRepository;
import org.example.inventoryservice.repository.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;


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

    private final InboxRepository inboxRepository;
    private final OutboxRepository outboxRepository;
    private final InventoryRepository inventoryRepository;
    private final OutboxDlqService outboxDlqService;
    private final ObjectMapper objectMapper;

    /**
     * Processes inventory check request.
     *
     * <p>
     * This method evaluates inventory availability and store outbox event.
     * </p>
     *
     * @param event inventory check request event
     */
    @Transactional
    public void processInventory(InventoryCheckRequestedEvent event) {
        int inserted = inboxRepository.insertIfNotExists(event.messageId());

        if (inserted == 0) {
            log.warn("[INVENTORY-SERVICE] Event {} already processed.", event.messageId());
            return;
        }

        log.info("[INVENTORY-SERVICE] Processing inventory for order {}", event.orderId());


        for (OrderItemEvent item : event.items()) {

            InventoryItem inventory = inventoryRepository
                    .findById(item.productId())
                    .orElse(null);

            if (inventory == null) {
                log.warn("[INVENTORY-SERVICE] Item {} not found.", item.productId());
                storeOutboxEventFailure(event, "ITEM_NOT_FOUND");
                return;
            }

            if (!inventory.canReserve(item.quantity())) {

                log.warn("[INVENTORY-SERVICE] Not enough stock for product {} requested {} available {}",
                        item.productId(),
                        item.quantity(),
                        inventory.getAvailableQuantity()
                );

                storeOutboxEventFailure(event, "OUT_OF_STOCK");

                return;
            }


            inventory.reserve(item.quantity());
            inventoryRepository.save(inventory);
        }


        storeOutboxEventSuccess(event);
    }

    private void storeOutboxEventSuccess(InventoryCheckRequestedEvent event) {
        InventoryReservedEvent payload = new InventoryReservedEvent(
                event.orderId(),
                event.correlationId(),
                UUID.randomUUID()
        );

        storeOutbox(payload, EventConstants.EVENT_INVENTORY_RESERVED, event.orderId());
    }

    private void storeOutboxEventFailure(InventoryCheckRequestedEvent event, String reason) {

        InventoryFailedEvent payload = new InventoryFailedEvent(
                event.orderId(),
                reason,
                event.correlationId(),
                UUID.randomUUID()
        );

        storeOutbox(payload, EventConstants.EVENT_INVENTORY_FAILED, event.orderId());
    }

    private void storeOutbox(Object payload, String eventType, Long aggregateId) {

        try {
            OutboxEvent event = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateType("INVENTORY")
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .processed(Boolean.FALSE)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxRepository.save(event);

            log.info("[INVENTORY-SERVICE] Stored outbox event {} ({})", event.getId(), eventType);

        } catch (Exception e) {
            log.error("[INVENTORY-SERVICE] Failed to serialize payload for event {} (aggregateId={}). Moved to DLQ table.",
                    eventType, aggregateId, e);
            outboxDlqService.storeOutboxDlq(null, aggregateId, eventType,
                    payload, 0, e);
        }
    }
}
