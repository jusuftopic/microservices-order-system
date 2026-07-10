package org.example.messagingstarter.contracts;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event emitted by Order Service to request payment processing.
 *
 * <p>This event is sent only after successful inventory reservation.</p>
 *
 * <p>Consumed by Payment Service.</p>
 */
public record PaymentRequestedEvent(

        /**
         * Unique identifier of the order.
         */
        Long orderId,

        /**
         * Total amount to be charged.
         */
        BigDecimal amount,

        /**
         * Customer email associated with the order.
         */
        String customerEmail,

        /**
         * Correlation identifier used for tracking the workflow.
         */
        String correlationId,

        /**
         * Unique identifier of the message.
         */
        UUID messageId
) implements BaseEvent {}

