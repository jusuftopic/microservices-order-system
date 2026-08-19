package org.example.messagingstarter.contracts.lifecycle;

import java.util.Arrays;
import java.util.Optional;

/**
 * Stable business reasons for authoritative order lifecycle transitions.
 */
public enum LifecycleReasonCode {
    ORDER_STATUS_UPDATED,
    INVENTORY_RESERVED,
    INVENTORY_RESERVATION_FAILED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    INVENTORY_COMMITTED,
    ORDER_WORKFLOW_COMPLETED,
    COMPENSATION_COMPLETED,
    INVENTORY_COMMIT_FAILED,
    PAYMENT_REFUND_REQUIRED,
    ORDER_PROCESSING_TIMEOUT;

    /**
     * Returns the value written to the lifecycle event contract.
     *
     * @return stable reason code
     */
    public String code() {
        return name();
    }

    /**
     * Resolves a known reason without rejecting newer contract values.
     *
     * @param code serialized reason code
     * @return matching reason, when known by this application version
     */
    public static Optional<LifecycleReasonCode> fromCode(String code) {
        return Arrays.stream(values())
                .filter(reason -> reason.code().equals(code))
                .findFirst();
    }
}
