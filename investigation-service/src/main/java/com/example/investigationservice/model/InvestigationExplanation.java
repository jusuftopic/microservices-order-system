package com.example.investigationservice.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Explanation selected for an investigation response.
 *
 * @param text generated explanation, when available
 * @param source strategy that produced the selected explanation
 */
public record InvestigationExplanation(
        Optional<String> text,
        Source source
) {

    public InvestigationExplanation {
        text = Objects.requireNonNull(text, "text must not be null");
        source = Objects.requireNonNull(source, "source must not be null");
    }

    /**
     * Creates a result for which no explanation is available.
     *
     * @return unavailable explanation result
     */
    public static InvestigationExplanation unavailable() {
        return new InvestigationExplanation(Optional.empty(), Source.NONE);
    }

    /**
     * Identifies the mechanism that produced an explanation.
     */
    public enum Source {
        AI,
        DETERMINISTIC,
        NONE
    }
}
