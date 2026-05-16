package org.example.orderservice.dto.response;

/**
 * Response DTO for Order Items.
 */
public record OrderItemResponse(
        Long productId,
        Integer quantity
) {
}
