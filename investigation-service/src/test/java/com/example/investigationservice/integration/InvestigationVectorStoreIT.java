package com.example.investigationservice.integration;

import com.example.investigationservice.model.InvestigationKnowledgeDocument;
import com.example.investigationservice.model.InvestigationKnowledgeMatch;
import com.example.investigationservice.service.knowledge.InvestigationKnowledgeStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.ai.model.embedding.text=none",
        "spring.ai.vectorstore.type=pgvector",
        "spring.ai.vectorstore.pgvector.initialize-schema=true",
        "spring.ai.vectorstore.pgvector.dimensions=3",
        "spring.ai.vectorstore.pgvector.index-type=HNSW",
        "spring.ai.vectorstore.pgvector.distance-type=COSINE_DISTANCE",
        "spring.ai.vectorstore.pgvector.table-name=investigation_knowledge_test"
})
@Import(InvestigationVectorStoreIT.TestEmbeddingConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class InvestigationVectorStoreIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg15")
                    .asCompatibleSubstituteFor("postgres")
    )
            .withDatabaseName("investigation")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private InvestigationKnowledgeStore knowledgeStore;

    @Test
    void storesAndRetrievesSemanticallySimilarKnowledge() {
        InvestigationKnowledgeDocument compensation = new InvestigationKnowledgeDocument(
                "payment-failure-compensation",
                "Payment failed after inventory reservation. Inventory release is required.",
                Map.of("sourceId", "payment-failure-compensation", "version", 1)
        );
        InvestigationKnowledgeDocument notification = new InvestigationKnowledgeDocument(
                "order-lifecycle-happy-path",
                "A completed order can request a customer notification.",
                Map.of("sourceId", "order-lifecycle-happy-path", "version", 1)
        );

        knowledgeStore.store(List.of(compensation, notification));

        List<InvestigationKnowledgeMatch> matches = knowledgeStore.search(
                "How should a payment failure be compensated?",
                1,
                0.8
        );

        assertThat(matches).singleElement().satisfies(match -> {
            assertThat(match.document().content()).isEqualTo(compensation.content());
            assertThat(match.document().metadata())
                    .containsEntry("sourceId", "payment-failure-compensation")
                    .containsEntry("version", 1);
            assertThat(match.score()).isGreaterThanOrEqualTo(0.8);
        });
    }

    @TestConfiguration
    static class TestEmbeddingConfiguration {

        @Bean
        EmbeddingModel embeddingModel() {
            return new DeterministicEmbeddingModel();
        }
    }

    static class DeterministicEmbeddingModel implements EmbeddingModel {

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = IntStream.range(
                            0,
                            request.getInstructions().size()
                    )
                    .mapToObj(index -> new Embedding(
                            vectorFor(request.getInstructions().get(index)),
                            index
                    ))
                    .toList();
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return vectorFor(document.getText());
        }

        @Override
        public int dimensions() {
            return 3;
        }

        private float[] vectorFor(String text) {
            String normalized = text.toLowerCase(Locale.ROOT);
            if (normalized.contains("payment fail")
                    || normalized.contains("inventory release")) {
                return new float[]{1.0f, 0.0f, 0.0f};
            }
            if (normalized.contains("notification")) {
                return new float[]{0.0f, 1.0f, 0.0f};
            }
            return new float[]{0.0f, 0.0f, 1.0f};
        }
    }
}
