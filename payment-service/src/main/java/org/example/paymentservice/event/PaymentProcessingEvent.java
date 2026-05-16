package org.example.paymentservice.event;

/**
 * Internal event representing that a payment is ready to be processed
 * by an external payment provider.
 *
 * IMPORTANT: This is NOT persisted — it is only used for async workflow.
 */
public record PaymentProcessingEvent(
        Long paymentId,
        Long orderId,
        String correlationId
){
}
