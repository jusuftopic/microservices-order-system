package com.example.investigationservice.dto.response;

import java.util.List;

/**
 * Answer produced for an order investigation question.
 *
 * @param orderId investigated order identifier
 * @param answer answer grounded in the order timeline and retrieved knowledge
 * @param currentStatus authoritative current order status
 * @param referencedKnowledgeIds knowledge documents referenced by the answer
 */
public record InvestigationQuestionResponse(
        long orderId,
        String answer,
        String currentStatus,
        List<String> referencedKnowledgeIds
) {
}
