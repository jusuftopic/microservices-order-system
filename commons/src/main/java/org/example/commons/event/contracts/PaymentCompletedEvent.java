package org.example.commons.event.contracts;

/**
 * Event emitted by Payment Service when payment has been successfully processed.
 *
 * <p>This indicates that the transaction was successfully completed.</p>
 *
 * <p>Consumed by Order Service to mark the order as completed.</p>
 */
public record PaymentCompletedEvent(

        /**
         * Unique identifier of the order.
         */
        Long orderId,

        /**
         * Correlation identifier used for tracing the workflow.
         */
        String correlationId
) {}

