package org.example.messagingstarter.contracts.commands;

import org.example.messagingstarter.contracts.BaseEvent;
import org.example.messagingstarter.contracts.OrderItemEvent;

import java.util.List;
import java.util.UUID;

/**
 * Command emitted to request inventory validation and reservation.
 *
 * <p>This command represents the initiation of the inventory step in the order workflow.
 * It contains all items that must be checked and potentially reserved.</p>
 *
 * <p>Consumed by Inventory Service.</p>
 */
public record ReserveInventoryCommand(

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

