package org.example.orderservice.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating orders.
 */
public record OrderRequest(

        @NotBlank(message = "Customer email is required")
        @Email(message = "Invalid email format")
        String customerEmail,

        @NotEmpty(message = "Order must contain at least one item")
        List<OrderItemRequest> items,

        @Size(max = 255, message = "Description too long")
        String description
) {
}
