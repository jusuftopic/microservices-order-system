package com.example.investigationservice.service.explanation;

import com.example.investigationservice.metrics.InvestigationMetrics;
import com.example.investigationservice.model.InvestigationContext;
import com.example.investigationservice.model.InvestigationEvidence;
import com.example.investigationservice.model.InvestigationExplanation;
import com.example.investigationservice.service.explanation.ai.AiExplanationGenerator;
import com.example.investigationservice.service.explanation.deterministic.DeterministicExplanationGenerator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvestigationExplanationServiceTest {

    private static final String MISSING_RESPONSES_METRIC =
            "investigation.ai.responses.missing.total";
    private static final String INVALID_RESPONSES_METRIC =
            "investigation.ai.responses.invalid.total";
    private static final String REQUESTS_METRIC =
            "investigation.explanations.requests.total";
    private static final String AI_EXPLANATIONS_METRIC =
            "investigation.explanations.ai.total";
    private static final String DETERMINISTIC_EXPLANATIONS_METRIC =
            "investigation.explanations.deterministic.total";
    private static final String UNAVAILABLE_EXPLANATIONS_METRIC =
            "investigation.explanations.unavailable.total";

    @Test
    void recordsMissingResponseWithoutRecordingInvalidResponse() {
        TestFixture fixture = fixture(
                Optional.empty(),
                Optional.of("Deterministic explanation")
        );

        InvestigationExplanation explanation = fixture.service().explain(context());

        assertThat(explanation.source())
                .isEqualTo(InvestigationExplanation.Source.DETERMINISTIC);
        assertThat(fixture.registry().get(MISSING_RESPONSES_METRIC).counter().count())
                .isEqualTo(1.0);
        assertThat(fixture.registry().get(INVALID_RESPONSES_METRIC).counter().count())
                .isZero();
        assertFinalOutcomes(fixture.registry(), 0.0, 1.0, 0.0);
    }

    @Test
    void recordsInvalidResponseWithoutRecordingMissingResponse() {
        TestFixture fixture = fixture(
                Optional.of("   "),
                Optional.of("Deterministic explanation")
        );

        InvestigationExplanation explanation = fixture.service().explain(context());

        assertThat(explanation.source())
                .isEqualTo(InvestigationExplanation.Source.DETERMINISTIC);
        assertThat(fixture.registry().get(MISSING_RESPONSES_METRIC).counter().count())
                .isZero();
        assertThat(fixture.registry().get(INVALID_RESPONSES_METRIC).counter().count())
                .isEqualTo(1.0);
        assertFinalOutcomes(fixture.registry(), 0.0, 1.0, 0.0);
    }

    @Test
    void recordsValidatedAiResponseAsFinalOutcome() {
        TestFixture fixture = fixture(
                Optional.of("AI explanation"),
                Optional.of("Deterministic explanation")
        );

        InvestigationExplanation explanation = fixture.service().explain(context());

        assertThat(explanation.source()).isEqualTo(InvestigationExplanation.Source.AI);
        assertThat(fixture.registry().get(MISSING_RESPONSES_METRIC).counter().count())
                .isZero();
        assertThat(fixture.registry().get(INVALID_RESPONSES_METRIC).counter().count())
                .isZero();
        assertFinalOutcomes(fixture.registry(), 1.0, 0.0, 0.0);
    }

    @Test
    void recordsUnavailableWhenNeitherSourceProducesExplanation() {
        TestFixture fixture = fixture(Optional.empty(), Optional.empty());

        InvestigationExplanation explanation = fixture.service().explain(context());

        assertThat(explanation.source())
                .isEqualTo(InvestigationExplanation.Source.NONE);
        assertFinalOutcomes(fixture.registry(), 0.0, 0.0, 1.0);
    }

    private TestFixture fixture(
            Optional<String> aiResponse,
            Optional<String> deterministicResponse
    ) {
        AiExplanationGenerator aiGenerator = mock(AiExplanationGenerator.class);
        DeterministicExplanationGenerator deterministicGenerator =
                mock(DeterministicExplanationGenerator.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        InvestigationContext context = context();

        when(aiGenerator.generate(context)).thenReturn(aiResponse);
        when(deterministicGenerator.generate(context))
                .thenReturn(deterministicResponse);

        InvestigationExplanationService service = new InvestigationExplanationService(
                aiGenerator,
                new ExplanationValidationService(),
                deterministicGenerator,
                new InvestigationMetrics(registry)
        );
        return new TestFixture(service, registry);
    }

    private void assertFinalOutcomes(
            SimpleMeterRegistry registry,
            double ai,
            double deterministic,
            double unavailable
    ) {
        assertThat(registry.get(REQUESTS_METRIC).counter().count()).isEqualTo(1.0);
        assertThat(registry.get(AI_EXPLANATIONS_METRIC).counter().count()).isEqualTo(ai);
        assertThat(registry.get(DETERMINISTIC_EXPLANATIONS_METRIC).counter().count())
                .isEqualTo(deterministic);
        assertThat(registry.get(UNAVAILABLE_EXPLANATIONS_METRIC).counter().count())
                .isEqualTo(unavailable);
    }

    private InvestigationContext context() {
        InvestigationEvidence evidence = new InvestigationEvidence(
                "PAYMENT_SERVICE",
                "EVENT_PAYMENT_FAILED",
                "INVENTORY_RESERVE_COMPLETED",
                "PAYMENT_FAILED",
                "PAYMENT_FAILED",
                "RELEASE_INVENTORY",
                "INVENTORY_SERVICE",
                true,
                "INVENTORY_RELEASE",
                Instant.parse("2026-08-19T10:05:00Z"),
                1
        );
        return new InvestigationContext(42L, "PAYMENT_FAILED", List.of(evidence));
    }

    private record TestFixture(
            InvestigationExplanationService service,
            SimpleMeterRegistry registry
    ) {
    }
}
