package org.example.paymentservice.dto;

import org.example.paymentservice.enums.PaymentProviderStatus;

/**
 * DTO represents payment result after interaction with 3rd party payment provider
 */
public record PaymentResultDTO(
        PaymentProviderStatus status,
        String transactionId,
        String failureReason,
        String nextActionType,
        String provider
) {

    public boolean success() {
        return status == PaymentProviderStatus.SUCCEEDED;
    }
}
