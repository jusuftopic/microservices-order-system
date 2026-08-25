package com.example.investigationservice.service.explanation;

import com.example.investigationservice.model.AiExplanationResponse;
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
     * @param candidate structured generated response
     * @param context evidence used during generation
     * @return {@code true} when the candidate can be exposed
     */
    public boolean isValid(
            AiExplanationResponse candidate,
            InvestigationContext context
    ) {
        return candidate != null
                && candidate.explanation() != null
                && !candidate.explanation().isBlank()
                && context.hasEvidence();
    }
}
