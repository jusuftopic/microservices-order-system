package org.example.paymentservice.dto;

/**
 * DTO represents payment result after interaction with 3rd party payment provider
 */
public record PaymentResultDTO(
        boolean success,
        String transactionId,
        String failureReason,
        String provider
) {
}
