package com.example.investigationservice.metrics;

import io.micrometer.core.instrument.Counter;
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
    private final MeterRegistry registry;

    /**
     * Registers metrics that describe lifecycle evidence processing.
     *
     * @param registry application meter registry
     */
    public InvestigationMetrics(MeterRegistry registry) {
        this.registry = registry;
        lifecycleConcurrentInserts = Counter.builder(CONCURRENT_INSERTS_METRIC)
                .description("Concurrent lifecycle evidence inserts detected by message ID")
                .register(registry);
        deterministicExplanations = Counter.builder(DETERMINISTIC_EXPLANATIONS_METRIC)
                .description("Deterministic explanations selected as fallback")
                .register(registry);
        unavailableExplanations = Counter.builder(UNAVAILABLE_EXPLANATIONS_METRIC)
                .description("Explanation requests completed without an available explanation")
                .register(registry);
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
        incrementAiMetric(AI_RESPONSES_MISSING_METRIC,
                "AI explanation requests completed without a response",
                promptVersion,
                provider,
                model);
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
        Counter.builder(AI_RESPONSES_INVALID_METRIC)
                .description("AI explanation responses rejected by validation")
                .tag("prompt_version", promptVersion)
                .tag("validation_reason", validationReason)
                .tag("provider", provider)
                .tag("model", model)
                .register(registry)
                .increment();
    }

    /**
     * Records a request entering explanation selection.
     *
     * @param provider configured AI provider
     * @param model configured AI model
     */
    public void recordExplanationRequest(String provider, String model) {
        incrementProviderMetric(
                EXPLANATION_REQUESTS_METRIC,
                "Explanation requests processed",
                provider,
                model
        );
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
        incrementAiMetric(AI_EXPLANATIONS_METRIC,
                "Validated AI explanations selected",
                promptVersion,
                provider,
                model);
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

    private void incrementAiMetric(
            String metricName,
            String description,
            String promptVersion,
            String provider,
            String model
    ) {
        Counter.builder(metricName)
                .description(description)
                .tag("prompt_version", promptVersion)
                .tag("provider", provider)
                .tag("model", model)
                .register(registry)
                .increment();
    }

    private void incrementProviderMetric(
            String metricName,
            String description,
            String provider,
            String model
    ) {
        Counter.builder(metricName)
                .description(description)
                .tag("provider", provider)
                .tag("model", model)
                .register(registry)
                .increment();
    }
}
