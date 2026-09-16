package org.example.paymentservice.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Internal event representing that a payment is ready to be processed
 * by an external payment provider.
 *
 */
public record PaymentProcessingEvent(
        Long paymentId,
        Long orderId,
        BigDecimal amount,
        String correlationId,
        UUID commandId
){
}
