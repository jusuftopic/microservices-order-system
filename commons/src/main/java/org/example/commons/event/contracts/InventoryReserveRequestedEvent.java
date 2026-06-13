package org.example.commons.event.contracts;

import java.util.List;
import java.util.UUID;

/**
 * Event emitted to request inventory validation and reservation.
 *
 * <p>This event represents the initiation of the inventory step in the order workflow.
 * It contains all items that must be checked and potentially reserved.</p>
 *
 * <p>Consumed by Inventory Service.</p>
 */
public record InventoryReserveRequestedEvent(

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

