package com.example.investigationservice.model;

/**
 * Provider-neutral structured response produced by an AI explanation adapter.
 * The response remains a candidate until it is validated against the supplied
 * investigation evidence.
 *
 * @param explanation human-readable order explanation
 * @param currentStatus current status represented in the explanation
 * @param reasonCode lifecycle reason represented in the explanation
 * @param decisionCode orchestration decision represented as an intended action
 * @param compensationType compensation represented in the explanation
 */
public record AiExplanationResponse(
        String explanation,
        String currentStatus,
        String reasonCode,
        String decisionCode,
        String compensationType
) {
}
