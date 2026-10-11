package com.example.investigationservice.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Question submitted for an order investigation.
 *
 * @param query question to answer from the order investigation
 */
public record InvestigationQuestionRequest(
        @NotBlank(message = "query must not be blank")
        String query
) {
}
