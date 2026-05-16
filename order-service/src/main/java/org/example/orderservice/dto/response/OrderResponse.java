package org.example.orderservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for Order.
 */
public record OrderResponse(
        Long id,
        String status,
        String customerEmail,
        List<OrderItemResponse> items,
        String description,
        LocalDateTime createdAt
) {
}
