package com.example.investigationservice.port.out;

import com.example.investigationservice.model.InvestigationContext;

import java.util.Optional;

/**
 * Provider-independent boundary for AI-supported order explanations.
 */
public interface AiExplanationGenerator {

    /**
     * Generates an explanation grounded in the supplied investigation context.
     *
     * @param context authoritative evidence available for the order
     * @return generated explanation, when the provider produces one
     */
    Optional<String> generate(InvestigationContext context);
}
