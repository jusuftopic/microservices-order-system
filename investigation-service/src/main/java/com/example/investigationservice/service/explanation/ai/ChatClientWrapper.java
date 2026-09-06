package com.example.investigationservice.service.explanation.ai;

import com.example.investigationservice.metrics.InvestigationMetrics;
import com.example.investigationservice.model.AiExplanationResponse;
import com.example.investigationservice.model.AiPrompt;
import com.google.genai.errors.ClientException;
import com.google.genai.errors.ServerException;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeoutException;

/**
 * Encapsulates communication with the configured language model.
 */
@Component
@ConditionalOnProperty(
        name = "app.ai.generator",
        havingValue = "model"
)
@Slf4j
public class ChatClientWrapper {

    private final ChatClient chatClient;
    private final InvestigationMetrics metrics;
    private final String provider;
    private final String model;
    private final Duration callBudget;
    private final Duration attemptTimeout;
    private final Duration initialBackoff;
    private final int maxAttempts;
    private final double jitter;

    /**
     * Creates the wrapper with the configured language-model client.
     *
     * @param chatClientBuilder configured language-model client builder
     * @param metrics model communication metrics
     * @param provider configured provider identifier
     * @param model configured model identifier
     * @param callBudget maximum duration reserved for all attempts
     * @param attemptTimeout maximum duration of one attempt
     * @param initialBackoff delay before the second attempt
     * @param maxAttempts maximum attempts including the initial call
     * @param jitter random backoff variation expressed as a fraction
     */
    public ChatClientWrapper(
            ChatClient.Builder chatClientBuilder,
            InvestigationMetrics metrics,
            @Value("${app.ai.provider}") String provider,
            @Value("${app.ai.model}") String model,
            @Value("${app.ai.timeout}") Duration callBudget,
            @Value("${app.ai.retry.attempt-timeout}") Duration attemptTimeout,
            @Value("${app.ai.retry.initial-backoff}") Duration initialBackoff,
            @Value("${app.ai.retry.max-attempts}") int maxAttempts,
            @Value("${app.ai.retry.jitter}") double jitter
    ) {
        this.chatClient = chatClientBuilder.build();
        this.metrics = metrics;
        this.provider = provider;
        this.model = model;
        this.callBudget = callBudget;
        this.attemptTimeout = attemptTimeout;
        this.initialBackoff = initialBackoff;
        this.maxAttempts = maxAttempts;
        this.jitter = jitter;

        if (maxAttempts < 1) {
            throw new IllegalArgumentException("AI max attempts must be at least one");
        }
        if (callBudget.isZero() || callBudget.isNegative()
                || attemptTimeout.isZero() || attemptTimeout.isNegative()) {
            throw new IllegalArgumentException("AI timeouts must be greater than zero");
        }
        if (attemptTimeout.compareTo(callBudget) > 0) {
            throw new IllegalArgumentException(
                    "AI attempt timeout must not exceed the complete call budget"
            );
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

    /**
     * Sends a prepared prompt to the model and maps its structured response.
     *
     * @param prompt validated prompt supplied to the model
     * @return structured response, when the model produces one
     */
    public Optional<AiExplanationResponse> generate(AiPrompt prompt) {
        long deadline = System.nanoTime() + callBudget.toNanos();
        AtomicInteger attempt = new AtomicInteger();
        Retry retry = createRetry(prompt);

        try {
            return retry.executeSupplier(() -> {
                int currentAttempt = attempt.incrementAndGet();
                ensureAttemptFitsWithinBudget(deadline, currentAttempt);
                try {
                    return invoke(prompt);
                } catch (RuntimeException exception) {
                    recordFailure(prompt, currentAttempt, exception);
                    throw exception;
                }
            });
        } catch (RuntimeException exception) {
            throwTimeoutIfApplicable(exception);
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
                                && classify(exception) == FailureType.TRANSIENT)
                .build();
        Retry retry = Retry.of("ai-explanation", retryConfig);

        retry.getEventPublisher()
                .onRetry(event -> {
                    metrics.recordAiRequestRetry(prompt.version(), provider, model);
                    log.warn(
                            "[INVESTIGATION-SERVICE][AI-CLIENT] Transient model failure on "
                                    + "attempt {} of {}; retrying after {} ms using provider {} "
                                    + "and model {}",
                            event.getNumberOfRetryAttempts(),
                            maxAttempts,
                            event.getWaitInterval().toMillis(),
                            provider,
                            model
                    );
                })
                .onError(event -> log.warn(
                        "[INVESTIGATION-SERVICE][AI-CLIENT] Transient model failure on attempt "
                                + "{} of {} using provider {} and model {}; attempts exhausted",
                        event.getNumberOfRetryAttempts(),
                        maxAttempts,
                        provider,
                        model
                ));
        return retry;
    }

    private void ensureAttemptFitsWithinBudget(long deadline, int attempt) {
        long remaining = deadline - System.nanoTime();
        if (remaining < attemptTimeout.toNanos()) {
            log.warn(
                    "[INVESTIGATION-SERVICE][AI-CLIENT] No time remains for attempt {} of {} "
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
        FailureType failureType = classify(exception);
        metrics.recordAiRequestFailure(
                prompt.version(), failureType.metricValue, provider, model
        );

        if (failureType == FailureType.NON_TRANSIENT) {
            log.error(
                    "[INVESTIGATION-SERVICE][AI-CLIENT] Non-transient model failure on "
                            + "attempt {} of {} using provider {} and model {}; no retry will "
                            + "be performed",
                    attempt,
                    maxAttempts,
                    provider,
                    model,
                    exception
            );
        } else if (failureType == FailureType.UNCLASSIFIED) {
            log.error(
                    "[INVESTIGATION-SERVICE][AI-CLIENT] Unclassified model failure on attempt "
                            + "{} of {} using provider {} and model {}; no retry will be performed",
                    attempt,
                    maxAttempts,
                    provider,
                    model,
                    exception
            );
        }
    }

    private Optional<AiExplanationResponse> invoke(AiPrompt prompt) {
        AiExplanationResponse response = chatClient.prompt()
                .system(prompt.systemInstructions())
                .user(prompt.userPrompt())
                .call()
                .entity(AiExplanationResponse.class);
        return Optional.ofNullable(response);
    }

    private FailureType classify(Throwable exception) {
        if (hasCause(exception, NonTransientAiException.class)) {
            return FailureType.NON_TRANSIENT;
        }
        ClientException clientException = findCause(exception, ClientException.class);
        if (clientException != null) {
            return clientException.code() == 408 || clientException.code() == 429
                    ? FailureType.TRANSIENT : FailureType.NON_TRANSIENT;
        }
        if (isTimeout(exception)
                || hasCause(exception, TransientAiException.class)
                || hasCause(exception, ServerException.class)
                || hasCause(exception, IOException.class)) {
            return FailureType.TRANSIENT;
        }
        return FailureType.UNCLASSIFIED;
    }

    private void throwTimeoutIfApplicable(RuntimeException exception) {
        if (isTimeout(exception)) {
            throw new ModelCallTimeoutException(exception);
        }
    }

    private boolean isTimeout(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof HttpTimeoutException
                    || cause instanceof SocketTimeoutException
                    || cause instanceof TimeoutException
                    || (cause instanceof InterruptedIOException
                    && cause.getMessage() != null
                    && cause.getMessage().toLowerCase().contains("timeout"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private <T extends Throwable> boolean hasCause(
            Throwable exception,
            Class<T> expectedType
    ) {
        return findCause(exception, expectedType) != null;
    }

    private <T extends Throwable> T findCause(
            Throwable exception,
            Class<T> expectedType
    ) {
        Throwable cause = exception;
        while (cause != null) {
            if (expectedType.isInstance(cause)) {
                return expectedType.cast(cause);
            }
            cause = cause.getCause();
        }
        return null;
    }

    private enum FailureType {
        TRANSIENT("transient"),
        NON_TRANSIENT("non_transient"),
        UNCLASSIFIED("unclassified");

        private final String metricValue;

        FailureType(String metricValue) {
            this.metricValue = metricValue;
        }
    }
}
