package org.example.inventoryservice.service;

import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.EventConstants;
import org.example.inventoryservice.entity.InventoryItem;
import org.example.inventoryservice.metrics.InventoryMetrics;
import org.example.inventoryservice.repository.InventoryRepository;
import org.example.inventoryservice.service.outbox.OutboxStoreService;
import org.example.messagingstarter.contracts.*;
import org.example.messagingstarter.contracts.commands.CommitInventoryCommand;
import org.example.messagingstarter.contracts.commands.ReleaseInventoryCommand;
import org.example.messagingstarter.contracts.commands.ReserveInventoryCommand;
import org.example.messagingstarter.contracts.events.*;
import org.example.messagingstarter.inbox.repository.InboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;


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
    private final InventoryRepository inventoryRepository;
    private final OutboxStoreService outboxStoreService;
    private final InventoryMetrics metrics;

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
    public void processInventory(ReserveInventoryCommand event) {
        int inserted = inboxRepository.insertIfNotExists(event.messageId());

        if (inserted == 0) {
            logAlreadyProcessed(event.messageId());
            return;
        }

        incrementMetrics(metrics.getInventoryReservationsTotal());

        log.info("[INVENTORY-SERVICE] Processing inventory for order {}", event.orderId());


        Map<Long, Long> requestedQuantities = getRequestedQuantitiesMap(event.items());

        Map<Long, InventoryItem> inventoryByProductId = getInventoryByProductIdMap(requestedQuantities);

        for (Map.Entry<Long, Long> request : requestedQuantities.entrySet()) {
            InventoryItem inventory = inventoryByProductId.get(request.getKey());

            if (inventory == null) {
                log.warn("[INVENTORY-SERVICE] Item {} not found.", request.getKey());

                incrementMetrics(metrics.getInventoryReservationsFailedTotal());
                incrementMetrics(metrics.getInventoryItemNotFoundTotal());

                storeOutboxEventFailure(event, "ITEM_NOT_FOUND");
                return;
            }

            if (request.getValue() > Integer.MAX_VALUE
                    || !inventory.canReserve(request.getValue().intValue())) {

                log.warn("[INVENTORY-SERVICE] Not enough stock for product {} requested {} available {}. Order ID {}",
                        request.getKey(), request.getValue(), inventory.getAvailableQuantity(), event.orderId()
                );

                incrementMetrics(metrics.getInventoryReservationsFailedTotal());
                incrementMetrics(metrics.getInventoryOutOfStockTotal());

                storeOutboxEventFailure(event, "OUT_OF_STOCK");

                return;
            }
        }

        for (Map.Entry<Long, Long> request : requestedQuantities.entrySet()) {
            inventoryByProductId.get(request.getKey()).reserve(request.getValue().intValue());
        }

        inventoryRepository.saveAll(inventoryByProductId.values());

        incrementMetrics(metrics.getInventoryReservationsSuccessTotal());
        storeOutboxEventSuccess(event);
    }


    /**
     * Processes inventory commit request.
     *
     * <p>
     * This method finalizes previously reserved inventory for an order.
     * It reduces the reserved quantity and confirms that stock has been
     * permanently deducted.
     * </p>
     *
     * <p>
     * If any item is not found, the commit operation fails and a failure
     * event is stored. Otherwise, a success event is emitted.
     * </p>
     *
     * @param event inventory commit request event
     */
    @Transactional
    public void processCommit(CommitInventoryCommand event) {

        int inserted = inboxRepository.insertIfNotExists(event.messageId());

        if (inserted == 0) {
            logAlreadyProcessed(event.messageId());
            return;
        }

        log.info("[INVENTORY-SERVICE] Committing inventory for order {}", event.orderId());

        Map<Long, Long> requestedQuantities = getRequestedQuantitiesMap(event.items());
        Map<Long, InventoryItem> inventoryByProductId = getInventoryByProductIdMap(requestedQuantities);

        for (Map.Entry<Long, Long> request : requestedQuantities.entrySet()) {
            InventoryItem inventory = inventoryByProductId.get(request.getKey());

            if (inventory == null) {
                log.warn("[INVENTORY-SERVICE] Cannot commit inventory for order {}: product {} not found",
                        event.orderId(), request.getKey());
                incrementMetrics(metrics.getInventoryCommitFailedTotal());
                incrementMetrics(metrics.getInventoryItemNotFoundTotal());
                storeCommitFailure(event, "ITEM_NOT_FOUND");
                return;
            }

            if (request.getValue() > inventory.getReservedQuantity()) {
                log.warn("[INVENTORY-SERVICE] Cannot commit inventory for order {}: product {} requested {} reserved {}",
                        event.orderId(), request.getKey(), request.getValue(), inventory.getReservedQuantity());
                incrementMetrics(metrics.getInventoryCommitFailedTotal());
                storeCommitFailure(event, "INSUFFICIENT_RESERVED_STOCK");
                return;
            }
        }

        for (Map.Entry<Long, Long> request : requestedQuantities.entrySet()) {
            InventoryItem inventory = inventoryByProductId.get(request.getKey());
            inventory.setReservedQuantity(
                    inventory.getReservedQuantity() - request.getValue().intValue()
            );
        }
        inventoryRepository.saveAll(inventoryByProductId.values());

        incrementMetrics(metrics.getInventoryCommitSuccessTotal());
        storeCommitSuccess(event);
    }


    /**
     * Processes inventory release request.
     *
     * <p>
     * This method performs a compensation step in the order workflow.
     * It releases previously reserved inventory and returns it back
     * to available stock.
     * </p>
     *
     * <p>
     * All requested items must have enough reserved stock before any stock is released.
     * A missing or insufficient reservation fails processing; no completion event is emitted.
     * </p>
     *
     * @param event inventory release request event
     */
    @Transactional
    public void processRelease(ReleaseInventoryCommand event) {

        int inserted = inboxRepository.insertIfNotExists(event.messageId());

        if (inserted == 0) {
            logAlreadyProcessed(event.messageId());
            return;
        }

        log.info("[INVENTORY-SERVICE] Releasing inventory for order {}", event.orderId());

        Map<Long, Long> requestedQuantities = getRequestedQuantitiesMap(event.items());
        Map<Long, InventoryItem> inventoryByProductId = getInventoryByProductIdMap(requestedQuantities);

        for (Map.Entry<Long, Long> request : requestedQuantities.entrySet()) {
            InventoryItem inventory = inventoryByProductId.get(request.getKey());

            if (inventory == null) {
                throw new IllegalArgumentException("Cannot release inventory for order " + event.orderId()
                        + ": product " + request.getKey() + " not found");
            }

            if (request.getValue() > inventory.getReservedQuantity()) {
                throw new IllegalArgumentException("Cannot release inventory for order " + event.orderId()
                        + ": product " + request.getKey() + " requested " + request.getValue()
                        + " but only " + inventory.getReservedQuantity() + " reserved");
            }
        }

        for (Map.Entry<Long, Long> request : requestedQuantities.entrySet()) {
            inventoryByProductId.get(request.getKey()).release(request.getValue().intValue());
        }
        inventoryRepository.saveAll(inventoryByProductId.values());

        incrementMetrics(metrics.getInventoryReleaseTotal());
        storeReleaseSuccess(event);
    }

    private Map<Long, Long> getRequestedQuantitiesMap(List<OrderItemEvent> items) {
        Map<Long, Long> requestedQuantities = new LinkedHashMap<>();
        items.forEach(
                item -> requestedQuantities.merge(item.productId(), item.quantity().longValue(), Long::sum));
        return requestedQuantities;
    }

    private Map<Long, InventoryItem> getInventoryByProductIdMap(Map<Long, Long> requestedQuantities) {
        return inventoryRepository
                .findAllById(requestedQuantities.keySet())
                .stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));
    }

    private void logAlreadyProcessed(UUID event) {
        log.warn("[INVENTORY-SERVICE] Event {} already processed.", event);
    }

    private void storeOutboxEventSuccess(ReserveInventoryCommand event) {
        InventoryReservedEvent payload = new InventoryReservedEvent(
                event.orderId(),
                event.correlationId(),
                UUID.randomUUID()
        );

        outboxStoreService.store(payload, EventConstants.EVENT_INVENTORY_RESERVED, event.orderId());
    }

    private void storeOutboxEventFailure(ReserveInventoryCommand event, String reason) {

        InventoryFailedEvent payload = new InventoryFailedEvent(
                event.orderId(),
                reason,
                event.correlationId(),
                UUID.randomUUID()
        );

        outboxStoreService.store(payload, EventConstants.EVENT_INVENTORY_FAILED, event.orderId());
    }

    private void storeCommitSuccess(CommitInventoryCommand event) {

        InventoryCommitCompletedEvent payload =
                new InventoryCommitCompletedEvent(
                        event.orderId(),
                        event.correlationId(),
                        UUID.randomUUID()
                );

        outboxStoreService.store(payload,
                EventConstants.EVENT_INVENTORY_COMMIT_COMPLETED,
                event.orderId());
    }

    private void storeCommitFailure(
            CommitInventoryCommand event,
            String reason
    ) {

        InventoryCommitFailedEvent payload =
                new InventoryCommitFailedEvent(
                        event.orderId(),
                        reason,
                        event.correlationId(),
                        UUID.randomUUID()
                );

        outboxStoreService.store(payload,
                EventConstants.EVENT_INVENTORY_COMMIT_FAILED,
                event.orderId());
    }


    private void storeReleaseSuccess(ReleaseInventoryCommand event) {

        InventoryReleaseCompletedEvent payload =
                new InventoryReleaseCompletedEvent(
                        event.orderId(),
                        event.correlationId(),
                        UUID.randomUUID()
                );

        outboxStoreService.store(payload,
                EventConstants.EVENT_INVENTORY_RELEASE_COMPLETED,
                event.orderId());
    }

    private void incrementMetrics(final Counter counter) {
        if (counter != null) counter.increment();
    }
}
