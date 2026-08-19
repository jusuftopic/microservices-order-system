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
    private final Counter aiResponsesMissing;
    private final Counter aiResponsesInvalid;
    private final Counter explanationRequests;
    private final Counter aiExplanations;
    private final Counter deterministicExplanations;
    private final Counter unavailableExplanations;

    /**
     * Registers metrics that describe lifecycle evidence processing.
     *
     * @param registry application meter registry
     */
    public InvestigationMetrics(MeterRegistry registry) {
        lifecycleConcurrentInserts = Counter.builder(CONCURRENT_INSERTS_METRIC)
                .description("Concurrent lifecycle evidence inserts detected by message ID")
                .register(registry);
        aiResponsesMissing = Counter.builder(AI_RESPONSES_MISSING_METRIC)
                .description("AI explanation requests completed without a response")
                .register(registry);
        aiResponsesInvalid = Counter.builder(AI_RESPONSES_INVALID_METRIC)
                .description("AI explanation responses rejected by validation")
                .register(registry);
        explanationRequests = Counter.builder(EXPLANATION_REQUESTS_METRIC)
                .description("Explanation requests processed")
                .register(registry);
        aiExplanations = Counter.builder(AI_EXPLANATIONS_METRIC)
                .description("Validated AI explanations selected")
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
     */
    public void recordMissingAiResponse() {
        aiResponsesMissing.increment();
    }

    /**
     * Records an AI response that failed explanation validation.
     */
    public void recordInvalidAiResponse() {
        aiResponsesInvalid.increment();
    }

    /**
     * Records a request entering explanation selection.
     */
    public void recordExplanationRequest() {
        explanationRequests.increment();
    }

    /**
     * Records a validated AI explanation selected for the response.
     */
    public void recordAiExplanation() {
        aiExplanations.increment();
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
