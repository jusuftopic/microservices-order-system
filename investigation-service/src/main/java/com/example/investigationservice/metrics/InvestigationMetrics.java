package com.example.investigationservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Central place for Investigation Service operational metrics.
 */
@Component
public class InvestigationMetrics {

    static final String CONCURRENT_INSERTS_METRIC =
            "investigation.lifecycle.concurrent.inserts.total";
    static final String AI_RESPONSES_MISSING_METRIC =
            "investigation.ai.responses.missing.total";
    static final String AI_RESPONSES_INVALID_METRIC =
            "investigation.ai.responses.invalid.total";
    static final String AI_REQUEST_TIMEOUTS_METRIC =
            "investigation.ai.requests.timeout.total";
    static final String AI_REQUEST_FAILURES_METRIC =
            "investigation.ai.requests.failures.total";
    static final String AI_REQUEST_RETRIES_METRIC =
            "investigation.ai.requests.retries.total";
    static final String AI_CIRCUIT_TRANSITIONS_METRIC =
            "investigation.ai.circuit.transitions.total";
    static final String EXPLANATION_REQUESTS_METRIC =
            "investigation.explanations.requests.total";
    static final String AI_EXPLANATIONS_METRIC =
            "investigation.explanations.ai.total";
    static final String DETERMINISTIC_EXPLANATIONS_METRIC =
            "investigation.explanations.deterministic.total";
    static final String UNAVAILABLE_EXPLANATIONS_METRIC =
            "investigation.explanations.unavailable.total";

    private final Counter lifecycleConcurrentInserts;
    private final Counter deterministicExplanations;
    private final Counter unavailableExplanations;
    private final Meter.MeterProvider<Counter> missingAiResponses;
    private final Meter.MeterProvider<Counter> invalidAiResponses;
    private final Meter.MeterProvider<Counter> aiRequestTimeouts;
    private final Meter.MeterProvider<Counter> aiRequestFailures;
    private final Meter.MeterProvider<Counter> aiRequestRetries;
    private final Meter.MeterProvider<Counter> aiCircuitTransitions;
    private final Meter.MeterProvider<Counter> explanationRequests;
    private final Meter.MeterProvider<Counter> aiExplanations;

    /**
     * Registers metrics that describe lifecycle evidence processing.
     *
     * @param registry application meter registry
     */
    public InvestigationMetrics(MeterRegistry registry) {
        lifecycleConcurrentInserts = Counter.builder(CONCURRENT_INSERTS_METRIC)
                .description("Concurrent lifecycle evidence inserts detected by message ID")
                .register(registry);
        deterministicExplanations = Counter.builder(DETERMINISTIC_EXPLANATIONS_METRIC)
                .description("Deterministic explanations selected as fallback")
                .register(registry);
        unavailableExplanations = Counter.builder(UNAVAILABLE_EXPLANATIONS_METRIC)
                .description("Explanation requests completed without an available explanation")
                .register(registry);
        missingAiResponses = Counter.builder(AI_RESPONSES_MISSING_METRIC)
                .description("AI explanation requests completed without a response")
                .withRegistry(registry);
        invalidAiResponses = Counter.builder(AI_RESPONSES_INVALID_METRIC)
                .description("AI explanation responses rejected by validation")
                .withRegistry(registry);
        aiRequestTimeouts = Counter.builder(AI_REQUEST_TIMEOUTS_METRIC)
                .description("AI explanation requests that exceeded their timeout")
                .withRegistry(registry);
        aiRequestFailures = Counter.builder(AI_REQUEST_FAILURES_METRIC)
                .description("Failed AI explanation request attempts")
                .withRegistry(registry);
        aiRequestRetries = Counter.builder(AI_REQUEST_RETRIES_METRIC)
                .description("AI explanation request retry attempts")
                .withRegistry(registry);
        aiCircuitTransitions = Counter.builder(AI_CIRCUIT_TRANSITIONS_METRIC)
                .description("AI circuit breaker state transitions")
                .withRegistry(registry);
        explanationRequests = Counter.builder(EXPLANATION_REQUESTS_METRIC)
                .description("Explanation requests processed")
                .withRegistry(registry);
        aiExplanations = Counter.builder(AI_EXPLANATIONS_METRIC)
                .description("Validated AI explanations selected")
                .withRegistry(registry);
    }

    /**
     * Records a unique-constraint conflict caused by concurrent processing of
     * the same lifecycle message.
     */
    public void recordConcurrentInsert() {
        lifecycleConcurrentInserts.increment();
    }

    /**
     * Records an AI generation attempt that completed without a response.
     *
     * @param promptVersion prompt contract used for the attempt
     * @param provider configured AI provider
     * @param model configured AI model
     */
    public void recordMissingAiResponse(
            String promptVersion,
            String provider,
            String model
    ) {
        missingAiResponses.withTags(
                "prompt_version", promptVersion,
                "provider", provider,
                "model", model
        ).increment();
    }

    /**
     * Records an AI response that failed explanation validation.
     *
     * @param promptVersion prompt contract that produced the response
     * @param validationReason bounded reason the response was rejected
     * @param provider configured AI provider
     * @param model configured AI model
     */
    public void recordInvalidAiResponse(
            String promptVersion,
            String validationReason,
            String provider,
            String model
    ) {
        invalidAiResponses.withTags(
                "prompt_version", promptVersion,
                "validation_reason", validationReason,
                "provider", provider,
                "model", model
        ).increment();
    }

    /**
     * Records a model request that exceeded its configured time budget.
     *
     * @param promptVersion prompt contract used for the request
     * @param provider configured AI provider
     * @param model configured AI model
     */
    public void recordAiRequestTimeout(
            String promptVersion,
            String provider,
            String model
    ) {
        aiRequestTimeouts.withTags(
                "prompt_version", promptVersion,
                "provider", provider,
                "model", model
        ).increment();
    }

    /**
     * Records a failed model attempt using a bounded failure classification.
     *
     * @param promptVersion prompt contract used for the attempt
     * @param failureType transient, non-transient or unclassified failure
     * @param provider configured AI provider
     * @param model configured AI model
     */
    public void recordAiRequestFailure(
            String promptVersion,
            String failureType,
            String provider,
            String model
    ) {
        aiRequestFailures.withTags(
                "prompt_version", promptVersion,
                "failure_type", failureType,
                "provider", provider,
                "model", model
        ).increment();
    }

    /**
     * Records an additional model attempt after a transient failure.
     *
     * @param promptVersion prompt contract used for the request
     * @param provider configured AI provider
     * @param model configured AI model
     */
    public void recordAiRequestRetry(
            String promptVersion,
            String provider,
            String model
    ) {
        aiRequestRetries.withTags(
                "prompt_version", promptVersion,
                "provider", provider,
                "model", model
        ).increment();
    }

    /**
     * Records a change in the model circuit breaker's operational state.
     *
     * @param fromState state before the transition
     * @param toState state after the transition
     * @param provider configured AI provider
     * @param model configured AI model
     */
    public void recordAiCircuitTransition(
            String fromState,
            String toState,
            String provider,
            String model
    ) {
        aiCircuitTransitions.withTags(
                "from_state", fromState,
                "to_state", toState,
                "provider", provider,
                "model", model
        ).increment();
    }

    /**
     * Records a request entering explanation selection.
     *
     * @param provider configured AI provider
     * @param model configured AI model
     */
    public void recordExplanationRequest(String provider, String model) {
        explanationRequests.withTags(
                "provider", provider,
                "model", model
        ).increment();
    }

    /**
     * Records a validated AI explanation selected for the response.
     *
     * @param promptVersion prompt contract that produced the explanation
     * @param provider configured AI provider
     * @param model configured AI model
     */
    public void recordAiExplanation(
            String promptVersion,
            String provider,
            String model
    ) {
        aiExplanations.withTags(
                "prompt_version", promptVersion,
                "provider", provider,
                "model", model
        ).increment();
    }

    /**
     * Records a deterministic explanation selected as fallback.
     */
    public void recordDeterministicExplanation() {
        deterministicExplanations.increment();
    }

    /**
     * Records a request for which neither explanation source produced output.
     */
    public void recordUnavailableExplanation() {
        unavailableExplanations.increment();
    }

}
