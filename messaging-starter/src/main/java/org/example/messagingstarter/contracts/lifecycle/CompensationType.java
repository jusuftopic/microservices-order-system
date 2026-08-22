package org.example.messagingstarter.contracts.lifecycle;

import java.util.Arrays;
import java.util.Optional;

/**
 * Compensating actions represented in order lifecycle evidence.
 */
public enum CompensationType {
    INVENTORY_RELEASE,
    PAYMENT_REFUND,
    STATUS_DEPENDENT_COMPENSATION;

    /**
     * Returns the value written to the lifecycle event contract.
     *
     * @return stable compensation code
     */
    public String code() {
        return name();
    }

    /**
     * Resolves a known compensation without rejecting newer contract values.
     *
     * @param code serialized compensation code
     * @return matching compensation, when known by this application version
     */
    public static Optional<CompensationType> fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code().equals(code))
                .findFirst();
    }
}
