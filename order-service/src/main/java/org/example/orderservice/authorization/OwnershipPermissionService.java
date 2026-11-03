package org.example.orderservice.authorization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.orderservice.metrics.OrderMetrics;
import org.example.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Makes fail-closed ownership decisions for Order API resources.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OwnershipPermissionService {

    private static final String RESOURCE_TYPE = "order";

    private final OrderRepository orderRepository;
    private final OrderMetrics metrics;

    /**
     * Checks whether a token customer claim owns an order.
     *
     * @param orderId requested order identifier
     * @param customerIdClaim signed customer identifier claim
     * @return {@code true} only for a valid identity and exact ownership match
     */
    @Transactional(readOnly = true)
    public boolean canAccess(Long orderId, String customerIdClaim) {
        UUID customerId = parseCustomerId(customerIdClaim);
        if (orderId == null || orderId <= 0 || customerId == null) {
            return deny(orderId, "invalid_identity_or_resource");
        }

        if (!orderRepository.existsByIdAndCustomerId(orderId, customerId)) {
            return deny(orderId, "not_owner_or_missing");
        }

        return true;
    }

    /**
     * Parses the application-controlled customer claim without exposing it in logs.
     *
     * @param customerIdClaim signed token claim
     * @return parsed identifier, or {@code null} when absent or malformed
     */
    private UUID parseCustomerId(String customerIdClaim) {
        try {
            return UUID.fromString(customerIdClaim);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return null;
        }
    }

    /**
     * Records and logs a denied ownership decision.
     *
     * @param orderId requested order identifier
     * @param reason bounded denial reason
     * @return always {@code false}
     */
    private boolean deny(Long orderId, String reason) {
        metrics.recordOwnershipAccessDenied(RESOURCE_TYPE, reason);
        log.warn(
                "[ORDER-SERVICE][AUTHORIZATION] Denied ownership access to order {} reason {}",
                orderId,
                reason
        );
        return false;
    }
}
