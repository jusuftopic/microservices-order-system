package com.example.investigationservice.model;

/**
 * Minimal authoritative order context used to enrich knowledge retrieval.
 */
public record OrderTimelineBrief(
        long orderId,
        String currentStatus,
        String reasonCode,
        String decisionCode,
        String compensationType,
        String sourceService
) {
}
