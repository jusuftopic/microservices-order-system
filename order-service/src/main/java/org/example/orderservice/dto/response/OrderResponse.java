package org.example.orderservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Order.
 */
public record OrderResponse(
        Long id,
        String status,
        String customerEmail,
        BigDecimal amount,
        String description,
        LocalDateTime createdAt
) {
}
