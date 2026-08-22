package com.example.investigationservice.model;

import java.util.List;
import java.util.Objects;

/**
 * Evidence-grounded input shared by all explanation strategies.
 *
 * @param orderId investigated order identifier
 * @param currentStatus latest status derived from collected evidence
 * @param evidence ordered lifecycle evidence
 */
public record InvestigationContext(
        long orderId,
        String currentStatus,
        List<InvestigationEvidence> evidence
) {

    public InvestigationContext {
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence must not be null"));
    }

    /**
     * Creates a context for an order without collected evidence.
     *
     * @param orderId investigated order identifier
     * @return empty investigation context
     */
    public static InvestigationContext empty(long orderId) {
        return new InvestigationContext(orderId, null, List.of());
    }

    /**
     * Indicates whether evidence is available for explanation generation.
     *
     * @return {@code true} when at least one evidence item is available
     */
    public boolean hasEvidence() {
        return !evidence.isEmpty();
    }
}
