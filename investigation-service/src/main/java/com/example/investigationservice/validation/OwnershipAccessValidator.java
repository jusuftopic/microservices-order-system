package com.example.investigationservice.validation;

import com.example.investigationservice.authorization.OwnershipPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapts authenticated JWT identity to Investigation ownership authorization.
 */
@Component("investigationOwnershipAccess")
@RequiredArgsConstructor
@Slf4j
public class OwnershipAccessValidator {

    private final OwnershipPermissionService permissionService;

    /**
     * Checks whether the authenticated customer may access an investigated order.
     *
     * @param authentication current authenticated principal
     * @param orderId requested order identifier
     * @return {@code true} only for an exact ownership match
     */
    public boolean canAccessOrder(Authentication authentication, Long orderId) {
        // Do not remove "unused method" - used in @PreAuthorized method annotation
        return permissionService.canAccessOrder(orderId, customerIdClaim(authentication));
    }

    /**
     * Checks whether the authenticated customer may access a question execution.
     *
     * @param authentication current authenticated principal
     * @param executionId public question execution identifier
     * @return {@code true} only when its associated order belongs to the customer
     */
    public boolean canAccessQuestion(Authentication authentication, UUID executionId) {
        return permissionService.canAccessQuestion(
                executionId,
                customerIdClaim(authentication)
        );
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
