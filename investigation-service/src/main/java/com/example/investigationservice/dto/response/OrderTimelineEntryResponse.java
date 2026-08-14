package com.example.investigationservice.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Lifecycle evidence exposed as part of an order investigation.
 *
 * @param messageId             identifier of the lifecycle event
 * @param causationId           identifier of the event or command that caused the transition
 * @param correlationId         identifier shared by the distributed order flow
 * @param sourceService         service that originated the transition
 * @param sourceEventType       event type that caused the transition
 * @param previousStatus        authoritative status before the transition
 * @param newStatus             authoritative status after the transition
 * @param reasonCode            stable reason for the transition
 * @param orchestrationDecision next action selected by the orchestrator, when applicable
 * @param compensation          compensation evidence associated with the transition
 * @param occurredAt            time at which the transition occurred
 * @param eventVersion          lifecycle event contract version
 */
public record OrderTimelineEntryResponse(
        UUID messageId,
        UUID causationId,
        String correlationId,
        String sourceService,
        String sourceEventType,
        String previousStatus,
        String newStatus,
        String reasonCode,
        OrchestrationDecisionResponse orchestrationDecision,
        CompensationResponse compensation,
        Instant occurredAt,
        int eventVersion
) {

    /**
     * Orchestration action selected as a result of the lifecycle transition.
     *
     * @param code action code
     * @param targetService service asked to perform the action
     * @param commandId identifier of the emitted command
     */
    public record OrchestrationDecisionResponse(
            String code,
            String targetService,
            UUID commandId
    ) {
    }

    /**
     * Compensation requirement associated with the lifecycle transition.
     *
     * @param required whether compensation is required
     * @param type compensation type, when required
     */
    public record CompensationResponse(
            boolean required,
            String type
    ) {
    }
}
