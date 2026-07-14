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


        for (OrderItemEvent item : event.items()) {

            InventoryItem inventory = inventoryRepository
                    .findById(item.productId())
                    .orElse(null);

            if (inventory == null) {
                logItemNotFound(item);

                incrementMetrics(metrics.getInventoryReservationsFailedTotal());
                incrementMetrics(metrics.getInventoryItemNotFoundTotal());

                storeOutboxEventFailure(event, "ITEM_NOT_FOUND");
                return;
            }

            if (!inventory.canReserve(item.quantity())) {

                log.warn("[INVENTORY-SERVICE] Not enough stock for product {} requested {} available {}. Order ID {}",
                        item.productId(),
                        item.quantity(),
                        inventory.getAvailableQuantity(),
                        event.orderId()
                );

                incrementMetrics(metrics.getInventoryReservationsFailedTotal());
                incrementMetrics(metrics.getInventoryOutOfStockTotal());

                storeOutboxEventFailure(event, "OUT_OF_STOCK");

                return;
            }


            inventory.reserve(item.quantity());
            inventoryRepository.save(inventory);
        }

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

        for (OrderItemEvent item : event.items()) {
            InventoryItem inventory = inventoryRepository
                    .findById(item.productId())
                    .orElse(null);

            if (inventory == null) {
                incrementMetrics(metrics.getInventoryCommitFailedTotal());
                incrementMetrics(metrics.getInventoryItemNotFoundTotal());
                storeCommitFailure(event, "ITEM_NOT_FOUND");
                return;
            }

            // finalize reservation
            inventory.setReservedQuantity(
                    inventory.getReservedQuantity() - item.quantity()
            );

            inventoryRepository.save(inventory);
        }

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
     * Missing items are skipped, and processing continues for the remaining items.
     * Once all items are processed, a success event is emitted.
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

        for (OrderItemEvent item : event.items()) {

            InventoryItem inventory = inventoryRepository
                    .findById(item.productId())
                    .orElse(null);

            if (inventory == null) {
                logItemNotFound(item);
                continue;
            }

            inventory.release(item.quantity());
            inventoryRepository.save(inventory);
        }

        incrementMetrics(metrics.getInventoryReleaseTotal());
        storeReleaseSuccess(event);
    }

    private void logItemNotFound(OrderItemEvent item) {
        log.warn("[INVENTORY-SERVICE] Item {} not found.", item.productId());
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
