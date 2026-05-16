package org.example.orderservice.enums;

/**
 * Represents lifecycle states of an Order.
 */
public enum OrderStatus {
    CREATED,

    INVENTORY_PROCESSING,
    INVENTORY_FAILED,

    PAYMENT_PROCESSING,
    PAYMENT_FAILED,

    COMPLETED,
    FAILED
}
