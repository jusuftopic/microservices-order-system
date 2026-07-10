package org.example.messagingstarter.contracts;

import java.util.List;
import java.util.UUID;

/**
 * Event emitted to request inventory release
 *
 * <p>This event represents one of the inventory steps in the order workflow.
 * It contains all items that must be released after failed payment.</p>
 *
 * <p>Consumed by Inventory Service.</p>
 */
public record InventoryReleasedRequestedEvent(

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
