package com.example.investigationservice.model;

import java.time.Instant;

/**
 * Provider-neutral lifecycle evidence used to build an investigation.
 */
public record InvestigationEvidence(
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
