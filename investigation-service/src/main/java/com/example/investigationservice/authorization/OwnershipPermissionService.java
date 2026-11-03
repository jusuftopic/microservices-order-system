package com.example.investigationservice.authorization;

import com.example.investigationservice.metrics.InvestigationMetrics;
import com.example.investigationservice.repository.OrderOwnerRepository;
import com.example.investigationservice.repository.QuestionExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Makes fail-closed ownership decisions for Investigation API resources.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OwnershipPermissionService {

    private final OrderOwnerRepository ownerRepository;
    private final QuestionExecutionRepository executionRepository;
    private final InvestigationMetrics metrics;

    /**
     * Checks whether a token customer claim owns an investigated order.
     *
     * @param orderId requested order identifier
     * @param customerIdClaim signed customer identifier claim
     * @return {@code true} only for an exact local ownership match
     */
    @Transactional(readOnly = true)
    public boolean canAccessOrder(Long orderId, String customerIdClaim) {
        UUID customerId = parseCustomerId(customerIdClaim);
        if (orderId == null || orderId <= 0 || customerId == null) {
            return deny("order", orderId, "invalid_identity_or_resource");
        }

        if (!ownerRepository.existsByOrderIdAndCustomerId(orderId, customerId)) {
            return deny("order", orderId, "not_owner_or_missing");
        }

        return true;
    }

    /**
     * Checks ownership of the order associated with a question execution.
     *
     * @param executionId public question execution identifier
     * @param customerIdClaim signed customer identifier claim
     * @return {@code true} only when the execution exists and its order is owned
     */
    @Transactional(readOnly = true)
    public boolean canAccessQuestion(UUID executionId, String customerIdClaim) {
        if (executionId == null) {
            return deny("question", null, "invalid_identity_or_resource");
        }

        Optional<Long> orderId = executionRepository.findOrderIdByExecutionId(executionId);
        if (orderId.isEmpty()) {
            return deny("question", null, "not_owner_or_missing");
        }

        UUID customerId = parseCustomerId(customerIdClaim);
        if (customerId == null) {
            return deny("question", orderId.get(), "invalid_identity_or_resource");
        }

        if (!ownerRepository.existsByOrderIdAndCustomerId(orderId.get(), customerId)) {
            return deny("question", orderId.get(), "not_owner_or_missing");
        }

        return true;
    }

    /**
     * Parses the application-controlled customer claim without logging it.
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
     * @param resourceType bounded resource classification
     * @param orderId protected order identifier when known
     * @param reason bounded denial reason
     * @return always {@code false}
     */
    private boolean deny(String resourceType, Long orderId, String reason) {
        metrics.recordOwnershipAccessDenied(resourceType, reason);
        log.warn(
                "[INVESTIGATION-SERVICE][AUTHORIZATION] Denied ownership access "
                        + "to {} for order {} reason {}",
                resourceType,
                orderId,
                reason
        );
        return false;
    }
}
