package org.example.messagingstarter.contracts;

import java.util.UUID;

/**
 * Event emitted to request payment refund.
 *
 * <p>
 * This event represents a compensation step in the order workflow.
 * It is triggered when an order fails after payment completion.
 * </p>
 *
 * <p>
 * Consumed by Payment Service.
 * </p>
 */
public record PaymentRefundRequestedEvent(
        Long orderId,
        String correlationId,
        UUID messageId
) implements BaseEvent {
}
