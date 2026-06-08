package org.example.paymentservice.event;

/**
 * Internal event representing that a payment is ready to be processed
 * by an external payment provider.
 *
 */
public record PaymentProcessingEvent(
        Long paymentId,
        Long orderId,
        String correlationId
){
}
