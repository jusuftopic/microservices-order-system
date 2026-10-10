package com.example.investigationservice.service.knowledge;

import com.example.investigationservice.model.InvestigationKnowledgeDocument;
import com.example.investigationservice.model.InvestigationKnowledgeMatch;

import java.util.List;

/**
 * Boundary for indexing and retrieving investigation knowledge.
 */
public interface InvestigationKnowledgeStore {

    /**
     * Stores or updates documents in the investigation knowledge index.
     *
     * @param documents documents to index
     */
    void store(List<InvestigationKnowledgeDocument> documents);

    /**
     * Finds knowledge that is semantically similar to a natural-language query.
     *
     * @param query natural-language investigation question
     * @param maxResults maximum number of results
     * @param minimumSimilarity lowest accepted similarity score
     * @return matches ordered by relevance
     */
    List<InvestigationKnowledgeMatch> search(
            String query,
            int maxResults,
            double minimumSimilarity
    );
}
