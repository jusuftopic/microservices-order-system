package org.example.orderservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.orderservice.dto.request.OrderRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    /**
     * Creates an order owned by the customer identified in the validated access token.
     *
     * @param jwt validated access token
     * @param request order creation request
     * @return persisted order
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OrderRequest request
    ) {
        Objects.requireNonNull(request, "request must not be null");

        OrderResponse response = service.createOrder(extractCustomerId(jwt), request);
        HttpStatus status = response.submitted()
                ? HttpStatus.CREATED
                : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Extracts the internal customer identifier from an authenticated JWT.
     *
     * @param jwt validated access token
     * @return internal customer identifier
     * @throws AccessDeniedException when the required claim is missing or malformed
     */
    private UUID extractCustomerId(Jwt jwt) {
        if (jwt == null) {
            throw new AccessDeniedException("Authenticated customer identity is required");
        }

        String customerId = jwt.getClaimAsString("customer_id");
        try {
            return UUID.fromString(customerId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AccessDeniedException(
                    "Authenticated token must contain a valid customer_id claim",
                    exception
            );
        }
    }

    /**
     * Retrieves an order by ID.
     *
     * @param id order ID
     * @return Order
     */
    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return service.getOrder(id);
    }
}
