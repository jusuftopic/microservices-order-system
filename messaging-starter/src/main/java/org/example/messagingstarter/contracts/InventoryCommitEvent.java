package org.example.messagingstarter.contracts;

import java.util.List;
import java.util.UUID;

/**
 * Event emitted to request inventory validation and commit.
 *
 * <p>This event represents the initiation of the inventory commit step in the order workflow.
 * It contains all items which availability must be commited.</p>
 *
 * <p>Consumed by Inventory Service.</p>
 */
public record InventoryCommitEvent(
        /**
         * Unique identifier of the order.
         */
        Long orderId,

        /**
         * List of items included in the order.
         */
        List<OrderItemEvent> items,

        /**
         * Correlation identifier used for tracking the workflow across services.
         */
        String correlationId,


        /**
         * Unique identifier of the message.
         */
        UUID messageId
 ) implements BaseEvent {
}
