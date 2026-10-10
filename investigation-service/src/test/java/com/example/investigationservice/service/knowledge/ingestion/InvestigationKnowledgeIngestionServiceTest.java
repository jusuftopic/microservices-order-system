package com.example.investigationservice.service.knowledge.ingestion;

import com.example.investigationservice.metrics.InvestigationMetrics;
import com.example.investigationservice.model.InvestigationKnowledgeDocument;
import com.example.investigationservice.model.InvestigationKnowledgeMatch;
import com.example.investigationservice.service.knowledge.InvestigationKnowledgeStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.dao.TransientDataAccessResourceException;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvestigationKnowledgeIngestionServiceTest {

    private static final String CORPUS_VERSION = "order-investigation-v1";

    @Test
    void retriesTransientWriteAndVerifiesCompleteCorpus() {
        FakeKnowledgeStore store = new FakeKnowledgeStore(true, false);
        InvestigationKnowledgeIngestionService service = service(store);

        service.ingest();

        assertThat(store.storeAttempts()).isEqualTo(10);
        assertThat(store.storedIds()).hasSize(9);
    }

    @Test
    void failsWhenPostWriteVerificationFindsMissingDocument() {
        FakeKnowledgeStore store = new FakeKnowledgeStore(false, true);
        InvestigationKnowledgeIngestionService service = service(store);

        assertThatThrownBy(service::ingest)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Corpus ingestion failed")
                .cause()
                .hasMessageStartingWith(
                        "Corpus ingestion verification failed"
                );
    }

    @Test
    void failsWhenDistributedLockIsNotAcquired() {
        FakeKnowledgeStore store = new FakeKnowledgeStore(false, false);
        InvestigationMetrics metrics = new InvestigationMetrics(
                new SimpleMeterRegistry()
        );
        InvestigationKnowledgeIngestionService service =
                new InvestigationKnowledgeIngestionService(
                        loader(),
                        store,
                        ingestion -> false,
                        retryExecutor(metrics),
                        metrics,
                        CORPUS_VERSION
                );

        assertThatThrownBy(service::ingest)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Another corpus ingestion job holds the database lock");
        assertThat(store.storeAttempts()).isZero();
    }

    @Test
    void doesNotRetryNonTransientWriteFailure() {
        FakeKnowledgeStore store = new FakeKnowledgeStore(
                new IllegalArgumentException("invalid document"),
                false
        );
        InvestigationKnowledgeIngestionService service = service(store);

        assertThatThrownBy(service::ingest)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Corpus ingestion failed")
                .cause()
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(store.storeAttempts()).isEqualTo(1);
    }

    private InvestigationKnowledgeIngestionService service(
            FakeKnowledgeStore store
    ) {
        InvestigationMetrics metrics = new InvestigationMetrics(
                new SimpleMeterRegistry()
        );
        return new InvestigationKnowledgeIngestionService(
                loader(),
                store,
                ingestion -> {
                    ingestion.run();
                    return true;
                },
                retryExecutor(metrics),
                metrics,
                CORPUS_VERSION
        );
    }

    private KnowledgeCorpusLoader loader() {
        return new KnowledgeCorpusLoader(
                new PathMatchingResourcePatternResolver()
        );
    }

    private KnowledgeIngestionRetryExecutor retryExecutor(
            InvestigationMetrics metrics
    ) {
        return new KnowledgeIngestionRetryExecutor(
                metrics,
                3,
                Duration.ofMillis(1),
                0.0
        );
    }

    private static class FakeKnowledgeStore
            implements InvestigationKnowledgeStore {

        private final RuntimeException firstWriteFailure;
        private final boolean omitOneDuringVerification;
        private final AtomicBoolean firstWrite = new AtomicBoolean(true);
        private final AtomicInteger attempts = new AtomicInteger();
        private final Set<String> storedIds = new HashSet<>();

        FakeKnowledgeStore(
                boolean failFirstWrite,
                boolean omitOneDuringVerification
        ) {
            this(failFirstWrite
                            ? new TransientDataAccessResourceException(
                                    "temporary vector-store failure"
                            ) : null,
                    omitOneDuringVerification);
        }

        FakeKnowledgeStore(
                RuntimeException firstWriteFailure,
                boolean omitOneDuringVerification
        ) {
            this.firstWriteFailure = firstWriteFailure;
            this.omitOneDuringVerification = omitOneDuringVerification;
        }

        @Override
        public void store(List<InvestigationKnowledgeDocument> documents) {
            attempts.incrementAndGet();
            if (firstWriteFailure != null
                    && firstWrite.compareAndSet(true, false)) {
                throw firstWriteFailure;
            }
            documents.stream()
                    .map(InvestigationKnowledgeDocument::id)
                    .forEach(storedIds::add);
        }

        @Override
        public Set<String> getStoredDocumentIds(String corpusVersion) {
            if (!omitOneDuringVerification) {
                return Set.copyOf(storedIds);
            }
            return storedIds.stream().skip(1).collect(
                    java.util.stream.Collectors.toSet()
            );
        }

        @Override
        public List<InvestigationKnowledgeMatch> search(
                String query,
                int maxResults,
                double minimumSimilarity
        ) {
            return List.of();
        }

        int storeAttempts() {
            return attempts.get();
        }

        Set<String> storedIds() {
            return Set.copyOf(storedIds);
        }
    }
}
