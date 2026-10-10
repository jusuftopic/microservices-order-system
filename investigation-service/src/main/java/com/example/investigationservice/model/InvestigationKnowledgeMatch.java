package com.example.investigationservice.model;

/**
 * Knowledge document returned by a semantic search with its similarity score.
 */
public record InvestigationKnowledgeMatch(
        InvestigationKnowledgeDocument document,
        double score
) {
}
