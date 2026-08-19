package org.example.messagingstarter.contracts.lifecycle;

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
}
