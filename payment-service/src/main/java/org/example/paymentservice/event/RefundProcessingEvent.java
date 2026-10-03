package org.example.paymentservice.event;

/**
 * Internal event indicating that a committed refund request is ready to be
 * sent to the payment provider.
 *
 * @param paymentId payment whose successful provider transaction is refunded
 */
public record RefundProcessingEvent(Long paymentId) {
}
