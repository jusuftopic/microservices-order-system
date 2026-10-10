package com.example.investigationservice.service.knowledge.ingestion;

import com.example.investigationservice.model.InvestigationKnowledgeDocument;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeCorpusLoaderTest {

    private final KnowledgeCorpusLoader loader = new KnowledgeCorpusLoader(
            new PathMatchingResourcePatternResolver()
    );

    @Test
    void loadsEffectiveDocumentsWithStableIngestionMetadata() {
        List<InvestigationKnowledgeDocument> documents = loader.load(
                "order-investigation-v1"
        );

        assertThat(documents).hasSize(9);
        assertThat(documents)
                .allSatisfy(document -> {
                    assertThat(document.id()).matches(
                            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
                    );
                    assertThat(document.content()).isNotBlank();
                    assertThat(document.metadata())
                            .containsEntry("corpusVersion", "order-investigation-v1")
                            .containsKeys(
                                    "documentId",
                                    "documentVersion",
                                    "contentHash",
                                    "source"
                            );
                    assertThat(document.metadata().get("contentHash").toString())
                            .matches("[0-9a-f]{64}");
                });
        assertThat(documents)
                .extracting(document -> document.metadata().get("source"))
                .doesNotContain("README.md");
    }
}
