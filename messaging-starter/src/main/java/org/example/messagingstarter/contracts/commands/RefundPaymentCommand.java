package org.example.messagingstarter.contracts.commands;

import org.example.messagingstarter.contracts.BaseEvent;

import java.util.UUID;

/**
 * Command emitted to request payment refund.
 *
 * <p>
 * This command represents a compensation step in the order workflow.
 * It is triggered when an order fails after payment completion.
 * </p>
 *
 * <p>
 * Consumed by Payment Service.
 * </p>
 */
public record RefundPaymentCommand(
        Long orderId,
        String correlationId,
        UUID messageId
) implements BaseEvent {
}
