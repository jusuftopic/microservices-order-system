package com.example.investigationservice.model;

import java.util.Objects;

/**
 * Provider-neutral prompt passed from the application to an AI adapter.
 *
 * @param version stable identifier of the prompt contract
 * @param systemInstructions behavioural and grounding rules for the model
 * @param userPrompt serialized order evidence to explain
 */
public record AiPrompt(
        String version,
        String systemInstructions,
        String userPrompt
) {

    public AiPrompt {
        version = Objects.requireNonNull(version, "version must not be null");
        systemInstructions = Objects.requireNonNull(
                systemInstructions,
                "systemInstructions must not be null"
        );
        userPrompt = Objects.requireNonNull(userPrompt, "userPrompt must not be null");
    }
}
