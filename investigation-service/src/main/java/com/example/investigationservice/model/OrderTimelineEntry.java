package com.example.investigationservice.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Complete business evidence for one entry in an order timeline.
 *
 * @param messageId lifecycle event identifier
 * @param causationId identifier of the triggering event
 * @param correlationId identifier shared by the distributed order flow
 * @param sourceService service that originated the triggering event
 * @param sourceEventType type of event that triggered the transition
 * @param previousStatus status before the transition
 * @param newStatus status after the transition
 * @param reasonCode business reason for the transition
 * @param orchestrationDecision next action selected by the orchestrator
 * @param compensation compensation associated with the transition
 * @param occurredAt time at which the transition occurred
 * @param eventVersion lifecycle event contract version
 */
public record OrderTimelineEntry(
        UUID messageId,
        UUID causationId,
        String correlationId,
        String sourceService,
        String sourceEventType,
        String previousStatus,
        String newStatus,
        String reasonCode,
        OrchestrationDecision orchestrationDecision,
        Compensation compensation,
        Instant occurredAt,
        int eventVersion
) {

    /**
     * Intended orchestration action attached to a transition.
     */
    public record OrchestrationDecision(
            String code,
            String targetService,
            UUID commandId
    ) {
    }

    /**
     * Compensation requirement attached to a transition.
     */
    public record Compensation(
            boolean required,
            String type
    ) {
    }
}
