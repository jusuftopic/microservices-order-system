package org.example.paymentservice.enums;

/**
 * Provider-neutral lifecycle returned after initiating a payment.
 */
public enum PaymentProviderStatus {
    PROCESSING,
    REQUIRES_ACTION,
    SUCCEEDED,
    FAILED,
    CANCELED
}
