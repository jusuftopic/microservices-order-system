package org.example.messagingstarter.contracts.lifecycle;

import java.util.Arrays;
import java.util.Optional;

/**
 * Stable order states represented in lifecycle evidence.
 */
public enum OrderStatus {
    CREATED,
    INVENTORY_RESERVE_COMPLETED,
    INVENTORY_RESERVE_FAILED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    INVENTORY_COMMIT_COMPLETED,
    INVENTORY_COMMIT_FAILED,
    COMPLETED,
    FAILED,
    TIMED_OUT;

    /**
     * Returns the value written to the lifecycle event contract.
     *
     * @return stable order status
     */
    public String code() {
        return name();
    }

    /**
     * Resolves a known lifecycle status.
     *
     * @param code serialized status code
     * @return matching status, when supported
     */
    public static Optional<OrderStatus> fromCode(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code().equals(code))
                .findFirst();
    }

    /**
     * Indicates whether the status ends order processing.
     *
     * @return {@code true} for a final order state
     */
    public boolean isFinalState() {
        return this == COMPLETED || this == FAILED || this == TIMED_OUT;
    }
}
