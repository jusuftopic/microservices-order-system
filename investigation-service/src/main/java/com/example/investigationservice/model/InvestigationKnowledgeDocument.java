package com.example.investigationservice.model;

import java.util.Map;

/**
 * Knowledge document available to order investigations.
 */
public record InvestigationKnowledgeDocument(
        String id,
        String content,
        Map<String, Object> metadata
) {
}
