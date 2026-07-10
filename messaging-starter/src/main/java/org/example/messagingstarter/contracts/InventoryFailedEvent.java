package org.example.messagingstarter.contracts;


import java.util.UUID;

/**
 * Event emitted when inventory reservation fails.
 *
 * <p>This indicates that one or more items are not available in sufficient quantity.</p>
 *
 * <p>Consumed by Order Service to mark the order as failed.</p>
 */
public record InventoryFailedEvent(

        /**
         * Unique identifier of the order.
         */
        Long orderId,

        /**
         * Reason describing why the inventory check failed.
         */
        String reason,

        /**
         * Correlation identifier used for tracing the workflow.
         */
        String correlationId,

        /**
         * Unique identifier of the message.
         */
        UUID messageId
) implements BaseEvent {}

