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
    FAILED
}
