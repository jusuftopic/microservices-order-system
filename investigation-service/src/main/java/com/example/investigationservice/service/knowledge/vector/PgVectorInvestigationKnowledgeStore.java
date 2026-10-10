package com.example.investigationservice.service.knowledge.vector;

import com.example.investigationservice.model.InvestigationKnowledgeDocument;
import com.example.investigationservice.model.InvestigationKnowledgeMatch;
import com.example.investigationservice.service.knowledge.InvestigationKnowledgeStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Stores and retrieves investigation knowledge through the configured
 * pgvector-backed vector store.
 */
@Component
@ConditionalOnProperty(name = "spring.ai.vectorstore.type", havingValue = "pgvector")
public class PgVectorInvestigationKnowledgeStore
        implements InvestigationKnowledgeStore {

    private static final Pattern SQL_IDENTIFIER =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final String corpusVersion;
    private final String qualifiedTableName;

    public PgVectorInvestigationKnowledgeStore(
            VectorStore vectorStore,
            JdbcTemplate jdbcTemplate,
            @Value("${app.rag.corpus.version}") String corpusVersion,
            @Value("${spring.ai.vectorstore.pgvector.schema-name}") String schemaName,
            @Value("${spring.ai.vectorstore.pgvector.table-name}") String tableName
    ) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.corpusVersion = corpusVersion;
        this.qualifiedTableName = getQualifiedTableName(schemaName, tableName);
    }

    @Override
    public void store(List<InvestigationKnowledgeDocument> documents) {
        vectorStore.add(documents.stream()
                .map(this::toVectorDocument)
                .toList());
    }

    @Override
    public Set<String> getStoredDocumentIds(String corpusVersion) {
        String sql = "SELECT id::text FROM " + qualifiedTableName
                + " WHERE metadata->>'corpusVersion' = ?";
        return Set.copyOf(jdbcTemplate.queryForList(
                sql,
                String.class,
                corpusVersion
        ));
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
                .filterExpression("corpusVersion == '"
                        + corpusVersion.replace("'", "''") + "'")
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

    private String getQualifiedTableName(String schemaName, String tableName) {
        if (!SQL_IDENTIFIER.matcher(schemaName).matches()
                || !SQL_IDENTIFIER.matcher(tableName).matches()) {
            throw new IllegalArgumentException("Invalid pgvector schema or table name");
        }
        return schemaName + "." + tableName;
    }
}
