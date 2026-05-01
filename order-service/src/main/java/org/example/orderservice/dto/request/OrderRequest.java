package org.example.orderservice.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request DTO for creating orders.
 */
public record OrderRequest(

        @NotBlank(message = "Customer email is required")
        @Email(message = "Invalid email format")
        String customerEmail,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be positive")
        BigDecimal amount,

        @Size(max = 255, message = "Description too long")
        String description
) {
}
