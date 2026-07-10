package org.example.messagingstarter.contracts;

import java.util.UUID;

/**
 * Event emitted by Payment Service when payment processing fails.
 *
 * <p>This may occur due to insufficient funds, provider rejection,
 * or unrecoverable errors.</p>
 *
 * <p>Consumed by Order Service to mark the order as failed and
 * potentially trigger compensation logic (e.g. inventory release).</p>
 */
public record PaymentFailedEvent(

        /**
         * Unique identifier of the order.
         */
        Long orderId,

        /**
         * Reason describing why the payment failed.
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

