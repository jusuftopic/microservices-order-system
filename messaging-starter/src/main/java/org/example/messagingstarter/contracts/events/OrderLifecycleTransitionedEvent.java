package org.example.messagingstarter.contracts.events;

import org.example.messagingstarter.contracts.BaseEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Fact emitted by the Order Service after an order lifecycle transition
 * has been committed to its local database.
 *
 * <p>The contract deliberately separates authoritative lifecycle facts from
 * an intended orchestration action. A decision in this event does not imply
 * that the target service completed the requested work.</p>
 */
public record OrderLifecycleTransitionedEvent(
        Long orderId,
        String previousStatus,
        String newStatus,
        String reasonCode,
        String sourceService,
        String sourceEventType,
        UUID causationId,
        OrchestrationDecision orchestrationDecision,
        Compensation compensation,
        Instant occurredAt,
        int eventVersion,
        String correlationId,
        UUID messageId
) implements BaseEvent {

    /**
     * Describes the next action selected by the saga orchestrator.
     */
    public record OrchestrationDecision(
            String code,
            String targetService,
            UUID commandId
    ) {}

    /**
     * Describes whether the transition requires compensating work.
     */
    public record Compensation(
            boolean required,
            String type
    ) {
        public static Compensation notRequired() {
            return new Compensation(false, null);
        }
    }
}
