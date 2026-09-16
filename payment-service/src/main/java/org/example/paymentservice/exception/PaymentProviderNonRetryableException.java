package org.example.paymentservice.exception;

import lombok.Getter;

/**
 * Represents a definitive provider rejection that requires changed input or
 * human intervention rather than an automatic retry.
 */
@Getter
public class PaymentProviderNonRetryableException extends RuntimeException {

    private final String provider;
    private final String providerRequestId;
    private final String errorCode;

    public PaymentProviderNonRetryableException(
            String message,
            String provider,
            String providerRequestId,
            String errorCode,
            Throwable cause
    ) {
        super(message, cause);
        this.provider = provider;
        this.providerRequestId = providerRequestId;
        this.errorCode = errorCode;
    }
}
