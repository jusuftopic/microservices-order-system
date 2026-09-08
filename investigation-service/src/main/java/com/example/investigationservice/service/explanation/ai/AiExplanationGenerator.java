package com.example.investigationservice.service.explanation.ai;

import com.example.investigationservice.model.AiExplanationResponse;
import com.example.investigationservice.model.InvestigationContext;

import java.util.Optional;

/**
 * Provider-independent boundary for AI-supported order explanations.
 */
public interface AiExplanationGenerator {

    /**
     * Identifies the configured model provider for operational measurements.
     *
     * @return stable, low-cardinality provider identifier
     */
    String provider();

    /**
     * Identifies the configured model for operational measurements.
     *
     * @return stable, low-cardinality model identifier
     */
    String model();

    /**
     * Identifies the prompt contract used by this generator.
     *
     * @return stable, controlled prompt version
     */
    String promptVersion();

    /**
     * Generates an explanation grounded in the supplied investigation context.
     *
     * @param context authoritative evidence available for the order
     * @return structured explanation candidate, when the provider produces one
     */
    Optional<AiExplanationResponse> generate(InvestigationContext context);
}
