package com.example.investigationservice.service.knowledge.ingestion;

import com.example.investigationservice.exception.KnowledgeIngestionLockException;
import com.example.investigationservice.metrics.InvestigationMetrics;
import com.example.investigationservice.model.InvestigationKnowledgeDocument;
import com.example.investigationservice.service.knowledge.InvestigationKnowledgeStore;
import com.example.investigationservice.service.knowledge.ingestion.lock.KnowledgeIngestionLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Coordinates one complete and repeatable knowledge-corpus ingestion.
 */
@Service
@ConditionalOnProperty(name = "app.rag.ingestion.enabled", havingValue = "true")
@Slf4j
public class InvestigationKnowledgeIngestionService {

    private final KnowledgeCorpusLoader corpusLoader;
    private final InvestigationKnowledgeStore knowledgeStore;
    private final KnowledgeIngestionLock ingestionLock;
    private final KnowledgeIngestionRetryExecutor retryExecutor;
    private final InvestigationMetrics metrics;
    private final String corpusVersion;

    private static final String RETRY_STORE_OPERATION = "store";
    private static final String RETRY_VERIFY_OPERATION = "verify";

    public InvestigationKnowledgeIngestionService(
            KnowledgeCorpusLoader corpusLoader,
            InvestigationKnowledgeStore knowledgeStore,
            KnowledgeIngestionLock ingestionLock,
            KnowledgeIngestionRetryExecutor retryExecutor,
            InvestigationMetrics metrics,
            @Value("${app.rag.corpus.version}") String corpusVersion
    ) {
        this.corpusLoader = corpusLoader;
        this.knowledgeStore = knowledgeStore;
        this.ingestionLock = ingestionLock;
        this.retryExecutor = retryExecutor;
        this.metrics = metrics;
        this.corpusVersion = corpusVersion;
    }

    public void ingest() {
        List<InvestigationKnowledgeDocument> documents = corpusLoader.load(corpusVersion);
        if (documents.isEmpty()) {
            fail("Corpus contains no effective knowledge documents", null);
        }

        boolean acquired;
        try {
            acquired = ingestionLock.execute(() -> ingestLocked(documents, getDocumentIds(documents)));
        } catch (KnowledgeIngestionLockException exception) {
            fail("Could not execute corpus ingestion under the database lock", exception);
            return;
        }
        if (!acquired) {
            fail("Another corpus ingestion job holds the database lock", null);
        }
    }

    private Set<String> getDocumentIds(List<InvestigationKnowledgeDocument> documents) {
        Set<String> expectedIds = documents.stream()
                .map(InvestigationKnowledgeDocument::id)
                .collect(java.util.stream.Collectors.toSet());
        if (expectedIds.size() != documents.size()) {
            fail("Corpus contains duplicate document identifiers", null);
        }
        return expectedIds;
    }

    private void ingestLocked(
            List<InvestigationKnowledgeDocument> documents,
            Set<String> expectedIds
    ) {
        log.info(
                "[INVESTIGATION-SERVICE][RAG-INGESTION] Ingesting {} documents "
                        + "for corpus {}",
                documents.size(),
                corpusVersion
        );
        try {
            for (InvestigationKnowledgeDocument document : documents) {
                retryExecutor.execute(corpusVersion, RETRY_STORE_OPERATION, () -> {
                    knowledgeStore.store(List.of(document));
                    return null;
                });
                metrics.recordRagIngestionDocument(corpusVersion);
            }

            Set<String> storedIds = retryExecutor.execute(
                    corpusVersion,
                    RETRY_VERIFY_OPERATION,
                    () -> knowledgeStore.getStoredDocumentIds(corpusVersion)
            );
            Set<String> missingIds = new HashSet<>(expectedIds);
            missingIds.removeAll(storedIds);
            Set<String> unexpectedIds = new HashSet<>(storedIds);
            unexpectedIds.removeAll(expectedIds);
            if (!missingIds.isEmpty() || !unexpectedIds.isEmpty()) {
                throw new IllegalStateException(
                        "Corpus ingestion verification failed; missing document IDs: "
                                + missingIds + ", unexpected document IDs: "
                                + unexpectedIds
                );
            }

            metrics.recordRagIngestionRun(corpusVersion, "success");
            log.info(
                    "[INVESTIGATION-SERVICE][RAG-INGESTION] Corpus {} successfully "
                            + "ingested and verified with {} documents",
                    corpusVersion,
                    documents.size()
            );
        } catch (RuntimeException exception) {
            fail("Corpus ingestion failed", exception);
        }
    }

    private void fail(String message, RuntimeException cause) {
        metrics.recordRagIngestionRun(corpusVersion, "failure");
        if (cause == null) {
            log.error("[INVESTIGATION-SERVICE][RAG-INGESTION] {} for corpus {}",
                    message, corpusVersion);
            throw new IllegalStateException(message);
        }
        log.error(
                "[INVESTIGATION-SERVICE][RAG-INGESTION] {} for corpus {}",
                message,
                corpusVersion,
                cause
        );
        throw new IllegalStateException(message, cause);
    }
}
