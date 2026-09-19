package org.example.paymentservice.enums;

/**
 * Represents the refund state of a payment.
 */
public enum RefundStatus {
    NOT_REQUESTED,
    REQUESTED,
    PROCESSING,
    SUCCESS,
    FAILED;

    public boolean isFinalState() {
        return this == SUCCESS || this == FAILED;
    }
}
