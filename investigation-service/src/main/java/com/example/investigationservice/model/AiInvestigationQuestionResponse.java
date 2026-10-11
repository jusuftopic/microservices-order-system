package com.example.investigationservice.model;

import java.util.List;

/**
 * Structured model response for an investigation question.
 */
public record AiInvestigationQuestionResponse(
        String answer,
        String currentStatus,
        List<String> referencedKnowledgeIds
) {
}
