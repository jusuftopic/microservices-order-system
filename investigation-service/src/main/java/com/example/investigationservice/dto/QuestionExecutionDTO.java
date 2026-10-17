package com.example.investigationservice.dto;

import com.example.investigationservice.entity.enums.ValidationOutcome;

import java.util.List;

public record QuestionExecutionDTO(
        Long orderId,
        String question,
        String answer,
        String promptVersion,
        ValidationOutcome validationOutcome,
        List<String> referencedKnowledgeIds
) {
}
