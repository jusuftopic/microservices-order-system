package com.example.investigationservice.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Provider-neutral lifecycle evidence used to build an investigation.
 */
public record InvestigationEvidence(
        UUID messageId,
        UUID causationId,
        String sourceService,
        String sourceEventType,
        String previousStatus,
        String newStatus,
        String reasonCode,
        String decisionCode,
        String decisionTargetService,
        boolean compensationRequired,
        String compensationType,
        Instant occurredAt,
        int eventVersion
) {
}
