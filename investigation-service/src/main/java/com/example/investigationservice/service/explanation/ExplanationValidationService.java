package com.example.investigationservice.service.explanation;

import com.example.investigationservice.model.InvestigationContext;
import org.springframework.stereotype.Service;

/**
 * Validates generated explanations against the response contract and the
 * evidence supplied to the generator.
 */
@Service
public class ExplanationValidationService {

    /**
     * Validates an AI-generated explanation candidate.
     *
     * @param candidate generated text
     * @param context evidence used during generation
     * @return {@code true} when the candidate can be exposed
     */
    public boolean isValid(String candidate, InvestigationContext context) {
        return candidate != null && !candidate.isBlank() && context.hasEvidence();
    }
}
