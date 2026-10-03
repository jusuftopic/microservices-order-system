package org.example.paymentservice.enums;

/**
 * Provider-neutral outcome reported for a refund operation.
 */
public enum RefundProviderStatus {
    SUCCEEDED,
    PROCESSING,
    FAILED,
    OUTCOME_UNKNOWN
}
