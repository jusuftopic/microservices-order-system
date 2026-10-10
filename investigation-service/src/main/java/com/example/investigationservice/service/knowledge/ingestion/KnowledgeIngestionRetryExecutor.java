package com.example.investigationservice.service.knowledge.ingestion;

import com.example.investigationservice.metrics.InvestigationMetrics;
import com.google.genai.errors.ClientException;
import com.google.genai.errors.ServerException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Applies bounded retry with backoff and jitter to ingestion operations.
 */
@Component
@ConditionalOnProperty(name = "app.rag.ingestion.enabled", havingValue = "true")
@Slf4j
public class KnowledgeIngestionRetryExecutor {

    private final InvestigationMetrics metrics;
    private final int maxAttempts;
    private final Duration initialBackoff;
    private final double jitter;

    public KnowledgeIngestionRetryExecutor(
            InvestigationMetrics metrics,
            @Value("${app.rag.ingestion.retry.max-attempts}") int maxAttempts,
            @Value("${app.rag.ingestion.retry.initial-backoff}") Duration initialBackoff,
            @Value("${app.rag.ingestion.retry.jitter}") double jitter
    ) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "RAG ingestion max attempts must be at least one"
            );
        }
        if (initialBackoff.isNegative()) {
            throw new IllegalArgumentException(
                    "RAG ingestion retry backoff must not be negative"
            );
        }
        if (jitter < 0.0 || jitter > 1.0) {
            throw new IllegalArgumentException(
                    "RAG ingestion retry jitter must be between zero and one"
            );
        }
        this.metrics = metrics;
        this.maxAttempts = maxAttempts;
        this.initialBackoff = initialBackoff;
        this.jitter = jitter;
    }

    public <T> T execute(
            String corpusVersion,
            String operation,
            Supplier<T> action
    ) {
        IntervalFunction intervals = jitter == 0.0
                ? IntervalFunction.of(initialBackoff)
                : IntervalFunction.ofRandomized(initialBackoff, jitter);
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .intervalFunction(intervals)
                .retryOnException(this::isTransient)
                .build();
        Retry retry = Retry.of("rag-ingestion-" + operation, config);
        retry.getEventPublisher().onRetry(event -> {
            metrics.recordRagIngestionRetry(corpusVersion, operation);
            log.warn(
                    "[INVESTIGATION-SERVICE][RAG-INGESTION] {} failed; "
                            + "retrying attempt {} of {} after {} ms",
                    operation,
                    event.getNumberOfRetryAttempts() + 1,
                    maxAttempts,
                    event.getWaitInterval().toMillis()
            );
        });
        return retry.executeSupplier(action);
    }

    private boolean isTransient(Throwable exception) {
        if (hasCause(exception, NonTransientAiException.class)) {
            return false;
        }
        ClientException clientException = findCause(exception, ClientException.class);
        if (clientException != null) {
            return clientException.code() == 408 || clientException.code() == 429;
        }
        return hasCause(exception, TransientAiException.class)
                || hasCause(exception, ServerException.class)
                || hasCause(exception, TransientDataAccessException.class)
                || hasCause(exception, RecoverableDataAccessException.class)
                || hasCause(exception, IOException.class);
    }

    private <T extends Throwable> boolean hasCause(
            Throwable exception,
            Class<T> type
    ) {
        return findCause(exception, type) != null;
    }

    private <T extends Throwable> T findCause(
            Throwable exception,
            Class<T> type
    ) {
        Throwable cause = exception;
        while (cause != null) {
            if (type.isInstance(cause)) {
                return type.cast(cause);
            }
            cause = cause.getCause();
        }
        return null;
    }
}
