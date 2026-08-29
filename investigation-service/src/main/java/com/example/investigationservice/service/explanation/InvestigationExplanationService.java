package com.example.investigationservice.service.explanation;

import com.example.investigationservice.metrics.InvestigationMetrics;
import com.example.investigationservice.model.AiExplanationResponse;
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
        metrics.recordExplanationRequest();
        String promptVersion = aiExplanationGenerator.promptVersion();

        try {
            Optional<AiExplanationResponse> candidate = aiExplanationGenerator.generate(context);
            if (candidate.isEmpty()) {
                metrics.recordMissingAiResponse(promptVersion);
                log.warn(
                        "[INVESTIGATION-SERVICE][EXPLANATION] AI generator returned no response "
                                + "for order {}; using fallback",
                        context.orderId()
                );
            } else if (validationService.isValid(candidate.get(), context)) {
                metrics.recordAiExplanation(promptVersion);
                log.debug("[INVESTIGATION-SERVICE][EXPLANATION] Selected AI explanation for order {}", context.orderId());
                return new InvestigationExplanation(
                        Optional.of(candidate.get().explanation()),
                        InvestigationExplanation.Source.AI
                );
            } else {
                metrics.recordInvalidAiResponse(promptVersion);
                log.warn(
                        "[INVESTIGATION-SERVICE][EXPLANATION] AI explanation validation failed "
                                + "for order {}; using fallback",
                        context.orderId()
                );
            }
        } catch (RuntimeException exception) {
            log.warn("[INVESTIGATION-SERVICE][EXPLANATION] AI explanation generation failed for order {}; using fallback",
                    context.orderId(), exception);
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
