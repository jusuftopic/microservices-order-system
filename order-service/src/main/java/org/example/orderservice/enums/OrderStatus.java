package org.example.orderservice.enums;

/**
 * Represents lifecycle states of an Order.
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
     * @return True if order status is in final state or not
     */
    public boolean isFinalState() {
        return this == OrderStatus.COMPLETED ||
                this == OrderStatus.FAILED ||
                this == OrderStatus.TIMED_OUT;
    }
}
