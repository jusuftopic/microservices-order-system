package org.example.orderservice.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response returned for an order lookup or submission.
 *
 * @param id persisted order identifier, or {@code null} when submission failed
 * @param status persisted lifecycle status, or {@code null} when submission failed
 * @param customerEmail customer contact email
 * @param items requested order items
 * @param description optional order description
 * @param createdAt persistence timestamp, or {@code null} when submission failed
 * @param submitted whether the order and its initial Outbox records were persisted
 */
public record OrderResponse(
        Long id,
        String status,
        String customerEmail,
        List<OrderItemResponse> items,
        String description,
        LocalDateTime createdAt,
        boolean submitted
) {
}
