package com.example.investigationservice.service;

import com.example.investigationservice.entity.OrderLifecycleEvidence;
import com.example.investigationservice.model.OrderTimeline;
import com.example.investigationservice.repository.OrderLifecycleEvidenceRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderTimelineReaderServiceTest {

    private final OrderLifecycleEvidenceRepository repository =
            mock(OrderLifecycleEvidenceRepository.class);
    private final OrderTimelineReaderService reader =
            new OrderTimelineReaderService(repository);

    @Test
    void returnsEmptyTimelineWhenNoEvidenceExists() {
        when(repository.findByOrderIdOrderByOccurredAtAscMessageIdAsc(42L))
                .thenReturn(List.of());

        OrderTimeline timeline = reader.read(42L);

        assertEquals(42L, timeline.orderId());
        assertFalse(timeline.hasEvidence());
        assertTrue(timeline.currentStatus().isEmpty());
        verify(repository).findByOrderIdOrderByOccurredAtAscMessageIdAsc(42L);
    }

    @Test
    void mapsCompleteEvidenceWithoutChangingRepositoryOrder() {
        UUID commandId = UUID.randomUUID();
        OrderLifecycleEvidence first = evidence(
                UUID.randomUUID(),
                "CREATED",
                "INVENTORY_RESERVE_COMPLETED",
                "INVENTORY_RESERVED",
                "PROCESS_PAYMENT",
                "PAYMENT_SERVICE",
                commandId,
                false,
                null,
                Instant.parse("2026-08-19T10:00:00Z")
        );
        OrderLifecycleEvidence second = evidence(
                UUID.randomUUID(),
                "INVENTORY_RESERVE_COMPLETED",
                "PAYMENT_FAILED",
                "PAYMENT_FAILED",
                "RELEASE_INVENTORY",
                "INVENTORY_SERVICE",
                UUID.randomUUID(),
                true,
                "INVENTORY_RELEASE",
                Instant.parse("2026-08-19T10:05:00Z")
        );
        when(repository.findByOrderIdOrderByOccurredAtAscMessageIdAsc(42L))
                .thenReturn(List.of(first, second));

        OrderTimeline timeline = reader.read(42L);

        assertEquals("PAYMENT_FAILED", timeline.currentStatus().orElseThrow());
        assertEquals(2, timeline.entries().size());
        assertEquals(first.getMessageId(), timeline.entries().get(0).messageId());
        assertEquals("correlation-42", timeline.entries().get(0).correlationId());
        assertEquals(commandId,
                timeline.entries().get(0).orchestrationDecision().commandId());
        assertFalse(timeline.entries().get(0).compensation().required());
        assertTrue(timeline.entries().get(1).compensation().required());
        assertEquals("INVENTORY_RELEASE", timeline.entries().get(1).compensation().type());
    }

    @Test
    void leavesDecisionEmptyWhenNoDecisionEvidenceExists() {
        OrderLifecycleEvidence evidence = evidence(
                UUID.randomUUID(),
                "CREATED",
                "INVENTORY_RESERVE_FAILED",
                "INVENTORY_RESERVATION_FAILED",
                null,
                null,
                null,
                false,
                null,
                Instant.parse("2026-08-19T10:00:00Z")
        );
        when(repository.findByOrderIdOrderByOccurredAtAscMessageIdAsc(42L))
                .thenReturn(List.of(evidence));

        OrderTimeline timeline = reader.read(42L);

        assertNull(timeline.entries().get(0).orchestrationDecision());
    }

    private OrderLifecycleEvidence evidence(
            UUID messageId,
            String previousStatus,
            String newStatus,
            String reasonCode,
            String decisionCode,
            String decisionTargetService,
            UUID decisionCommandId,
            boolean compensationRequired,
            String compensationType,
            Instant occurredAt
    ) {
        return OrderLifecycleEvidence.builder()
                .messageId(messageId)
                .orderId(42L)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reasonCode(reasonCode)
                .sourceService("INVENTORY_SERVICE")
                .sourceEventType("TEST_EVENT")
                .causationId(UUID.randomUUID())
                .decisionCode(decisionCode)
                .decisionTargetService(decisionTargetService)
                .decisionCommandId(decisionCommandId)
                .compensationRequired(compensationRequired)
                .compensationType(compensationType)
                .occurredAt(occurredAt)
                .eventVersion(1)
                .correlationId("correlation-42")
                .kafkaTopic("order.lifecycle.v1")
                .kafkaPartition(0)
                .kafkaOffset(1L)
                .build();
    }
}
