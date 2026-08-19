package com.example.investigationservice.service.projection;

import com.example.investigationservice.dto.response.OrderInvestigationResponse;
import com.example.investigationservice.model.InvestigationContext;
import com.example.investigationservice.model.InvestigationExplanation;
import com.example.investigationservice.model.OrderTimeline;
import com.example.investigationservice.model.OrderTimelineEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class InvestigationProjectionTest {

    private final OrderTimelineMapper timelineMapper = new OrderTimelineMapper();

    @Test
    void createsRestrictedExplanationContext() {
        OrderTimeline timeline = timeline();

        InvestigationContext context = timelineMapper.toInvestigationContext(timeline);

        assertEquals(42L, context.orderId());
        assertEquals("PAYMENT_COMPLETED", context.currentStatus());
        assertEquals(1, context.evidence().size());
        assertEquals("PAYMENT_COMPLETED", context.evidence().get(0).reasonCode());
        assertEquals("COMMIT_INVENTORY", context.evidence().get(0).decisionCode());
        assertFalse(context.evidence().get(0).compensationRequired());
    }

    @Test
    void createsCompleteApiTimelineWithTraceEvidence() {
        OrderTimeline timeline = timeline();
        InvestigationExplanation explanation = new InvestigationExplanation(
                Optional.of("Payment completed successfully."),
                InvestigationExplanation.Source.DETERMINISTIC
        );

        OrderInvestigationResponse response = timelineMapper.toResponse(
                timeline,
                explanation
        );

        assertEquals("PAYMENT_COMPLETED", response.currentStatus());
        assertEquals("Payment completed successfully.", response.explanation());
        assertEquals(timeline.entries().get(0).messageId(),
                response.timeline().get(0).messageId());
        assertEquals(timeline.entries().get(0).causationId(),
                response.timeline().get(0).causationId());
        assertEquals("correlation-42", response.timeline().get(0).correlationId());
        assertEquals(timeline.entries().get(0).orchestrationDecision().commandId(),
                response.timeline().get(0).orchestrationDecision().commandId());
        assertNull(response.timeline().get(0).compensation().type());
    }

    private OrderTimeline timeline() {
        OrderTimelineEntry entry = new OrderTimelineEntry(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "correlation-42",
                "PAYMENT_SERVICE",
                "EVENT_PAYMENT_SUCCESS",
                "INVENTORY_RESERVE_COMPLETED",
                "PAYMENT_COMPLETED",
                "PAYMENT_COMPLETED",
                new OrderTimelineEntry.OrchestrationDecision(
                        "COMMIT_INVENTORY",
                        "INVENTORY_SERVICE",
                        UUID.randomUUID()
                ),
                new OrderTimelineEntry.Compensation(false, null),
                Instant.parse("2026-08-19T10:00:00Z"),
                1
        );
        return new OrderTimeline(42L, List.of(entry));
    }
}
