package com.example.investigationservice.service.knowledge.vector;

import com.example.investigationservice.model.InvestigationKnowledgeDocument;
import com.example.investigationservice.model.InvestigationKnowledgeMatch;
import com.example.investigationservice.service.knowledge.InvestigationKnowledgeStore;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stores and retrieves investigation knowledge through the configured
 * pgvector-backed vector store.
 */
@Component
@ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "pgvector")
@RequiredArgsConstructor
public class PgVectorInvestigationKnowledgeStore
        implements InvestigationKnowledgeStore {

    private final VectorStore vectorStore;

    @Override
    public void store(List<InvestigationKnowledgeDocument> documents) {
        vectorStore.add(documents.stream()
                .map(this::toVectorDocument)
                .toList());
    }

    @Override
    public List<InvestigationKnowledgeMatch> search(
            String query,
            int maxResults,
            double minimumSimilarity
    ) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(maxResults)
                .similarityThreshold(minimumSimilarity)
                .build();

        return vectorStore.similaritySearch(request).stream()
                .map(this::toKnowledgeMatch)
                .toList();
    }

    private Document toVectorDocument(InvestigationKnowledgeDocument document) {
        return new Document(
                document.id(),
                document.content(),
                document.metadata()
        );
    }

    private InvestigationKnowledgeMatch toKnowledgeMatch(Document document) {
        InvestigationKnowledgeDocument knowledgeDocument =
                new InvestigationKnowledgeDocument(
                        document.getId(),
                        document.getText(),
                        document.getMetadata()
                );
        double score = document.getScore() == null ? 0.0 : document.getScore();
        return new InvestigationKnowledgeMatch(knowledgeDocument, score);
    }
}
