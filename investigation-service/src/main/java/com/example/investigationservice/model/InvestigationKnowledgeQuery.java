package com.example.investigationservice.model;

import java.util.Objects;

/**
 * Versioned query submitted to the investigation knowledge store.
 */
public record InvestigationKnowledgeQuery(
        String version,
        String text
) {

    public InvestigationKnowledgeQuery {
        version = Objects.requireNonNull(version, "version must not be null");
        text = Objects.requireNonNull(text, "text must not be null");
    }
}
