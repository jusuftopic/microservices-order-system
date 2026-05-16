package org.example.commons.event.contracts;

/**
 * Represents a single item within an order used in event communication.
 *
 * <p>This is a lightweight representation of order items used for
 * cross-service communication.</p>
 */
public record OrderItemEvent(

        /**
         * Identifier of the product.
         */
        Long productId,

        /**
         * Quantity of the product requested.
         */
        Integer quantity
) {}
