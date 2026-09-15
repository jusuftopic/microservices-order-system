package org.example.paymentservice.exception;

import lombok.Getter;

/**
 * Represents a temporary provider failure for which repeating the identical
 * idempotent operation can succeed.
 */
@Getter
public class PaymentProviderRetryableException extends RuntimeException {

    private final String provider;
    private final String providerRequestId;
    private final String errorCode;

    public PaymentProviderRetryableException(
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
