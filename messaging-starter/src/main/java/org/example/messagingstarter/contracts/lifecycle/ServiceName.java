package org.example.messagingstarter.contracts.lifecycle;

/**
 * Stable service identifiers used in lifecycle evidence.
 */
public enum ServiceName {
    ORDER_SERVICE,
    INVENTORY_SERVICE,
    PAYMENT_SERVICE,
    NOTIFICATION_SERVICE;

    /**
     * Returns the value written to the lifecycle event contract.
     *
     * @return stable service identifier
     */
    public String code() {
        return name();
    }
}
