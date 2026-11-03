package org.example.orderservice.validation;

import lombok.RequiredArgsConstructor;
import org.example.orderservice.authorization.OwnershipPermissionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Adapts authenticated JWT identity to Order API ownership authorization.
 */
@Component("orderOwnershipAccess")
@RequiredArgsConstructor
public class OwnershipAccessValidator {

    private final OwnershipPermissionService permissionService;

    /**
     * Checks whether the authenticated customer may access an order.
     *
     * @param authentication current authenticated principal
     * @param orderId requested order identifier
     * @return {@code true} only when the signed customer claim owns the order
     */
    public boolean canAccess(Authentication authentication, Long orderId) {
        return permissionService.canAccess(orderId, customerIdClaim(authentication));
    }

    /**
     * Reads the customer claim only from a validated JWT principal.
     *
     * @param authentication current authenticated principal
     * @return customer claim, or {@code null} for an unexpected principal
     */
    private String customerIdClaim(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        return jwt.getClaimAsString("customer_id");
    }
}
