package org.example.commons.event.contracts;

import java.util.UUID;

/**
 * Event emitted when inventory has been successfully reserved.
 *
 * <p>This indicates that all required items are available and reserved for the order.</p>
 *
 * <p>Consumed by Order Service to proceed with payment processing.</p>
 */
public record InventoryReservedEvent(

        /**
         * Unique identifier of the order.
         */
        Long orderId,

        /**
         * Correlation identifier used for tracing the workflow.
         */
        String correlationId,

        /**
         * Unique identifier of the message.
         */
        UUID messageId
) implements BaseEvent {}

