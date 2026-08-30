package com.example.investigationservice.service.explanation;

import com.example.investigationservice.metrics.InvestigationMetrics;
import com.example.investigationservice.model.AiExplanationResponse;
import com.example.investigationservice.model.ExplanationValidationResult;
import com.example.investigationservice.model.InvestigationContext;
import com.example.investigationservice.model.InvestigationExplanation;
import com.example.investigationservice.service.explanation.ai.AiExplanationGenerator;
import com.example.investigationservice.service.explanation.deterministic.DeterministicExplanationGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Selects a validated AI explanation or a deterministic fallback.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvestigationExplanationService {

    private final AiExplanationGenerator aiExplanationGenerator;
    private final ExplanationValidationService validationService;
    private final DeterministicExplanationGenerator deterministicGenerator;
    private final InvestigationMetrics metrics;

    /**
     * Produces the explanation selected for an investigation.
     *
     * @param context authoritative investigation context
     * @return selected explanation and its source
     */
    public InvestigationExplanation explain(InvestigationContext context) {
        String promptVersion = aiExplanationGenerator.promptVersion();
        String provider = aiExplanationGenerator.provider();
        String model = aiExplanationGenerator.model();
        metrics.recordExplanationRequest(provider, model);

        try {
            Optional<AiExplanationResponse> candidate = aiExplanationGenerator.generate(context);
            if (candidate.isEmpty()) {
                metrics.recordMissingAiResponse(promptVersion, provider, model);
                log.warn(
                        "[INVESTIGATION-SERVICE][EXPLANATION] AI generator returned no response "
                                + "for order {} using provider {} and model {}; using fallback",
                        context.orderId(),
                        provider,
                        model
                );
            } else {
                ExplanationValidationResult validation =
                        validationService.validate(candidate.get(), context);
                if (validation.valid()) {
                    metrics.recordAiExplanation(promptVersion, provider, model);
                    log.debug(
                            "[INVESTIGATION-SERVICE][EXPLANATION] "
                                    + "Selected AI explanation for order {} using provider {} and model {}",
                            context.orderId(),
                            provider,
                            model
                    );
                    return new InvestigationExplanation(
                            Optional.of(candidate.get().explanation()),
                            InvestigationExplanation.Source.AI
                    );
                }

                metrics.recordInvalidAiResponse(
                        promptVersion,
                        validation.failureReason().name(),
                        provider,
                        model
                );
            }
        } catch (RuntimeException exception) {
            log.warn(
                    "[INVESTIGATION-SERVICE][EXPLANATION] AI explanation generation failed "
                            + "for order {} using provider {} and model {}; using fallback",
                    context.orderId(),
                    provider,
                    model,
                    exception
            );
        }

        Optional<String> deterministic = deterministicGenerator.generate(context);
        if (deterministic.isPresent()) {
            metrics.recordDeterministicExplanation();
            log.debug("[INVESTIGATION-SERVICE][EXPLANATION] Selected deterministic explanation for order {}", context.orderId());
            return new InvestigationExplanation(deterministic, InvestigationExplanation.Source.DETERMINISTIC);
        }

        metrics.recordUnavailableExplanation();
        log.debug("[INVESTIGATION-SERVICE][EXPLANATION] No explanation is available for order {}", context.orderId());
        return InvestigationExplanation.unavailable();
    }
}
