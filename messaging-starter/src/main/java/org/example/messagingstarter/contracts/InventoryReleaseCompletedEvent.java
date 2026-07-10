package org.example.messagingstarter.contracts;

import java.util.UUID;


/**
 * Event emitted to indicate successful inventory release.
 *
 * <p>
 * This event represents the completion of a compensation step in the order workflow.
 * It signals that previously reserved inventory has been successfully released
 * and returned to available stock.
 * </p>
 *
 * <p>
 * Consumed by Order Service.
 * </p>
 */
public record InventoryReleaseCompletedEvent(

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
