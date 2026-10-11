package com.example.investigationservice.dto.response;

/**
 * Answer produced for an order investigation question.
 *
 * @param orderId investigated order identifier
 * @param answer answer grounded in the order timeline and retrieved knowledge
 */
public record InvestigationQuestionResponse(
        long orderId,
        String answer
) {
}
