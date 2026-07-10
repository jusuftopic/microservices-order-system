package org.example.messagingstarter.contracts.commands;

import org.example.messagingstarter.contracts.BaseEvent;
import org.example.messagingstarter.contracts.OrderItemEvent;

import java.util.List;
import java.util.UUID;

/**
 * Command emitted to request inventory validation and commit.
 *
 * <p>This command represents the initiation of the inventory commit step in the order workflow.
 * It contains all items which availability must be commited.</p>
 *
 * <p>Consumed by Inventory Service.</p>
 */
public record CommitInventoryCommand(
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
