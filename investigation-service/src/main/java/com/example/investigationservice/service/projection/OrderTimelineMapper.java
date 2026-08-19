package com.example.investigationservice.service.projection;

import com.example.investigationservice.dto.response.OrderInvestigationResponse;
import com.example.investigationservice.dto.response.OrderTimelineEntryResponse;
import com.example.investigationservice.model.InvestigationContext;
import com.example.investigationservice.model.InvestigationEvidence;
import com.example.investigationservice.model.InvestigationExplanation;
import com.example.investigationservice.model.OrderTimeline;
import com.example.investigationservice.model.OrderTimelineEntry;
import org.springframework.stereotype.Component;

/**
 * Maps the complete internal order timeline to its explanation and API representations.
 */
@Component
public class OrderTimelineMapper {

    /**
     * Selects the evidence made available to explanation generators.
     *
     * @param timeline complete internal order timeline
     * @return restricted evidence context for explanation generation
     */
    public InvestigationContext toInvestigationContext(OrderTimeline timeline) {
        return new InvestigationContext(
                timeline.orderId(),
                timeline.currentStatus().orElse(null),
                timeline.entries().stream()
                        .map(this::toInvestigationEvidence)
                        .toList()
        );
    }

    /**
     * Maps the complete timeline and selected explanation to the public API response.
     *
     * @param timeline complete internal order timeline
     * @param explanation selected human-readable explanation
     * @return public investigation response
     */
    public OrderInvestigationResponse toResponse(
            OrderTimeline timeline,
            InvestigationExplanation explanation
    ) {
        return new OrderInvestigationResponse(
                timeline.orderId(),
                timeline.hasEvidence(),
                timeline.currentStatus().orElse(null),
                explanation.text().orElse(null),
                timeline.entries().stream()
                        .map(this::toResponseEntry)
                        .toList()
        );
    }

    private InvestigationEvidence toInvestigationEvidence(OrderTimelineEntry entry) {
        OrderTimelineEntry.OrchestrationDecision decision = entry.orchestrationDecision();

        return new InvestigationEvidence(
                entry.messageId(),
                entry.causationId(),
                entry.sourceService(),
                entry.sourceEventType(),
                entry.previousStatus(),
                entry.newStatus(),
                entry.reasonCode(),
                decision == null ? null : decision.code(),
                decision == null ? null : decision.targetService(),
                entry.compensation().required(),
                entry.compensation().type(),
                entry.occurredAt(),
                entry.eventVersion()
        );
    }

    private OrderTimelineEntryResponse toResponseEntry(OrderTimelineEntry entry) {
        OrderTimelineEntry.OrchestrationDecision decision = entry.orchestrationDecision();

        return new OrderTimelineEntryResponse(
                entry.messageId(),
                entry.causationId(),
                entry.correlationId(),
                entry.sourceService(),
                entry.sourceEventType(),
                entry.previousStatus(),
                entry.newStatus(),
                entry.reasonCode(),
                decision == null
                        ? null
                        : new OrderTimelineEntryResponse.OrchestrationDecisionResponse(
                                decision.code(),
                                decision.targetService(),
                                decision.commandId()
                        ),
                new OrderTimelineEntryResponse.CompensationResponse(
                        entry.compensation().required(),
                        entry.compensation().type()
                ),
                entry.occurredAt(),
                entry.eventVersion()
        );
    }
}
