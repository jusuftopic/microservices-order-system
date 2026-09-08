package com.example.investigationservice.service.explanation.ai;

import com.example.investigationservice.exception.ModelCallTimeoutException;
import com.example.investigationservice.metrics.InvestigationMetrics;
import com.example.investigationservice.model.AiPrompt;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Executes model calls with bounded retry, backoff and jitter.
 */
@Component
@ConditionalOnProperty(name = "app.ai.generator", havingValue = "model")
@Slf4j
public class AiRetryExecutor {

    private final InvestigationMetrics metrics;
    private final AiFailureClassifier failureClassifier;
    private final String provider;
    private final String model;
    private final Duration callBudget;
    private final Duration attemptTimeout;
    private final Duration initialBackoff;
    private final int maxAttempts;
    private final double jitter;

    /**
     * Creates a retry boundary whose complete execution must fit within the
     * configured model-call budget.
     */
    public AiRetryExecutor(
            InvestigationMetrics metrics,
            AiFailureClassifier failureClassifier,
            @Value("${app.ai.provider}") String provider,
            @Value("${app.ai.model}") String model,
            @Value("${app.ai.timeout}") Duration callBudget,
            @Value("${app.ai.retry.attempt-timeout}") Duration attemptTimeout,
            @Value("${app.ai.retry.initial-backoff}") Duration initialBackoff,
            @Value("${app.ai.retry.max-attempts}") int maxAttempts,
            @Value("${app.ai.retry.jitter}") double jitter
    ) {
        this.metrics = metrics;
        this.failureClassifier = failureClassifier;
        this.provider = provider;
        this.model = model;
        this.callBudget = callBudget;
        this.attemptTimeout = attemptTimeout;
        this.initialBackoff = initialBackoff;
        this.maxAttempts = maxAttempts;
        this.jitter = jitter;
        validateConfiguration();
    }

    /**
     * Executes a model operation and retries only transient failures.
     *
     * @param prompt prompt metadata used for diagnostics
     * @param modelCall operation to execute
     * @param <T> operation result type
     * @return result returned by the operation
     */
    public <T> T execute(AiPrompt prompt, Supplier<T> modelCall) {
        long deadline = System.nanoTime() + callBudget.toNanos();
        AtomicInteger attempt = new AtomicInteger();
        Retry retry = createRetry(prompt);

        try {
            return retry.executeSupplier(() -> {
                int currentAttempt = attempt.incrementAndGet();
                ensureAttemptFitsWithinBudget(deadline, currentAttempt);
                try {
                    return modelCall.get();
                } catch (RuntimeException exception) {
                    recordFailure(prompt, currentAttempt, exception);
                    throw exception;
                }
            });
        } catch (RuntimeException exception) {
            if (failureClassifier.isTimeout(exception)) {
                throw new ModelCallTimeoutException(exception);
            }
            throw exception;
        }
    }

    private Retry createRetry(AiPrompt prompt) {
        IntervalFunction intervalFunction = jitter == 0.0
                ? IntervalFunction.of(initialBackoff)
                : IntervalFunction.ofRandomized(initialBackoff, jitter);
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .intervalFunction(intervalFunction)
                .retryOnException(exception ->
                        !(exception instanceof ModelCallTimeoutException)
                                && failureClassifier.classify(exception)
                                == AiFailureClassifier.FailureType.TRANSIENT)
                .build();
        Retry retry = Retry.of("ai-explanation", retryConfig);
        retry.getEventPublisher()
                .onRetry(event -> {
                    metrics.recordAiRequestRetry(prompt.version(), provider, model);
                    log.warn(
                            "[INVESTIGATION-SERVICE][AI-RETRY] Attempt {} of {} failed; "
                                    + "retrying after {} ms using provider {} and model {}",
                            event.getNumberOfRetryAttempts(),
                            maxAttempts,
                            event.getWaitInterval().toMillis(),
                            provider,
                            model
                    );
                })
                .onError(event -> log.warn(
                        "[INVESTIGATION-SERVICE][AI-RETRY] Attempt {} of {} failed using "
                                + "provider {} and model {}; attempts exhausted",
                        event.getNumberOfRetryAttempts(),
                        maxAttempts,
                        provider,
                        model
                ));
        return retry;
    }

    private void ensureAttemptFitsWithinBudget(long deadline, int attempt) {
        if (deadline - System.nanoTime() < attemptTimeout.toNanos()) {
            log.warn(
                    "[INVESTIGATION-SERVICE][AI-RETRY] No time remains for attempt {} of {} "
                            + "using provider {} and model {}",
                    attempt,
                    maxAttempts,
                    provider,
                    model
            );
            throw new ModelCallTimeoutException(
                    new TimeoutException("Insufficient model call budget")
            );
        }
    }

    private void recordFailure(
            AiPrompt prompt,
            int attempt,
            RuntimeException exception
    ) {
        AiFailureClassifier.FailureType failureType =
                failureClassifier.classify(exception);
        metrics.recordAiRequestFailure(
                prompt.version(), failureType.metricValue(), provider, model
        );
        if (failureType != AiFailureClassifier.FailureType.TRANSIENT) {
            log.error(
                    "[INVESTIGATION-SERVICE][AI-RETRY] {} model failure on attempt {} of {} "
                            + "using provider {} and model {}; no retry will be performed",
                    failureType,
                    attempt,
                    maxAttempts,
                    provider,
                    model,
                    exception
            );
        }
    }

    private void validateConfiguration() {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("AI max attempts must be at least one");
        }
        if (callBudget.isZero() || callBudget.isNegative()
                || attemptTimeout.isZero() || attemptTimeout.isNegative()) {
            throw new IllegalArgumentException("AI timeouts must be greater than zero");
        }
        if (initialBackoff.isNegative()) {
            throw new IllegalArgumentException("AI retry backoff must not be negative");
        }
        if (jitter < 0.0 || jitter > 1.0) {
            throw new IllegalArgumentException("AI retry jitter must be between zero and one");
        }
        double worstCaseDuration = maxAttempts * (double) attemptTimeout.toNanos()
                + (maxAttempts - 1) * initialBackoff.toNanos() * (1.0 + jitter);
        if (worstCaseDuration > callBudget.toNanos()) {
            throw new IllegalArgumentException(
                    "AI attempts and worst-case backoff must fit within the call budget"
            );
        }
    }
}
