package org.example.paymentservice.enums;

/**
 * Represents the state of a payment transaction.
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED;

    /**
     * Indicates whether the payment has reached an immutable outcome.
     *
     * @return {@code true} when regular processing must not change the status
     */
    public boolean isFinalState() {
        return this == SUCCESS || this == FAILED;
    }
}
