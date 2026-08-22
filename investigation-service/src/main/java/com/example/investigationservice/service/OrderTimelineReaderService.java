package com.example.investigationservice.service;

import com.example.investigationservice.entity.OrderLifecycleEvidence;
import com.example.investigationservice.model.OrderTimeline;
import com.example.investigationservice.model.OrderTimelineEntry;
import com.example.investigationservice.repository.OrderLifecycleEvidenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Supplies the ordered evidence context required by investigation queries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderTimelineReaderService {

    private final OrderLifecycleEvidenceRepository repository;

    /**
     * Reads the investigation context for an order.
     *
     * @param orderId investigated order identifier
     * @return complete timeline, which is empty when no evidence is available
     */
    @Transactional(readOnly = true)
    public OrderTimeline read(long orderId) {
        log.debug(
                "[INVESTIGATION-SERVICE][TIMELINE-READER] Reading timeline for order {}",
                orderId
        );

        var entries = repository
                .findByOrderIdOrderByOccurredAtAscMessageIdAsc(orderId)
                .stream()
                .map(this::toTimelineEntry)
                .toList();

        log.debug(
                "[INVESTIGATION-SERVICE][TIMELINE-READER] Read {} timeline entries for order {}",
                entries.size(),
                orderId
        );
        return new OrderTimeline(orderId, entries);
    }

    private OrderTimelineEntry toTimelineEntry(OrderLifecycleEvidence evidence) {
        OrderTimelineEntry.OrchestrationDecision decision = null;
        if (evidence.getDecisionCode() != null
                || evidence.getDecisionTargetService() != null
                || evidence.getDecisionCommandId() != null) {
            decision = new OrderTimelineEntry.OrchestrationDecision(
                    evidence.getDecisionCode(),
                    evidence.getDecisionTargetService(),
                    evidence.getDecisionCommandId()
            );
        }

        return new OrderTimelineEntry(
                evidence.getMessageId(),
                evidence.getCausationId(),
                evidence.getCorrelationId(),
                evidence.getSourceService(),
                evidence.getSourceEventType(),
                evidence.getPreviousStatus(),
                evidence.getNewStatus(),
                evidence.getReasonCode(),
                decision,
                new OrderTimelineEntry.Compensation(
                        evidence.isCompensationRequired(),
                        evidence.getCompensationType()
                ),
                evidence.getOccurredAt(),
                evidence.getEventVersion()
        );
    }
}
