package org.example.commons.event.contracts;

import java.util.UUID;

/**
 * Event emitted to indicate successful inventory commit.
 *
 * <p>
 * This event represents the finalization of the inventory step in the order workflow.
 * It signals that previously reserved inventory has been permanently deducted.
 * </p>
 *
 * <p>
 * Consumed by Order Service.
 * </p>
 */
public record InventoryCommitCompletedEvent(

        /**
         * Unique identifier of the order.
         */
        Long orderId,

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
