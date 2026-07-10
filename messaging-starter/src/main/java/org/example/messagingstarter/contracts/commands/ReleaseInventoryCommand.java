package org.example.messagingstarter.contracts.commands;

import org.example.messagingstarter.contracts.BaseEvent;
import org.example.messagingstarter.contracts.OrderItemEvent;

import java.util.List;
import java.util.UUID;

/**
 * Command emitted to request inventory release
 *
 * <p>This command represents one of the inventory steps in the order workflow.
 * It contains all items that must be released after failed payment.</p>
 *
 * <p>Consumed by Inventory Service.</p>
 */
public record ReleaseInventoryCommand(

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
