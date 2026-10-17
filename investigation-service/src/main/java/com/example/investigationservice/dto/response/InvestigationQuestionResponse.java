package com.example.investigationservice.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Answer produced for an order investigation question.
 *
 * @param orderId investigated order identifier
 * @param executionId persistent identifier for this question execution
 * @param answer answer grounded in the order timeline and retrieved knowledge
 * @param currentStatus authoritative current order status
 * @param referencedKnowledgeIds knowledge documents referenced by the answer
 */
public record InvestigationQuestionResponse(
        long orderId,
        UUID executionId,
        String answer,
        String currentStatus,
        List<String> referencedKnowledgeIds
) {
}
