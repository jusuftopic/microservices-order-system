package com.example.investigationservice.service.explanation;

import com.example.investigationservice.model.InvestigationContext;
import com.example.investigationservice.model.InvestigationExplanation;
import com.example.investigationservice.port.out.AiExplanationGenerator;
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

    /**
     * Produces the explanation selected for an investigation.
     *
     * @param context authoritative investigation context
     * @return selected explanation and its source
     */
    public InvestigationExplanation explain(InvestigationContext context) {
        try {
            Optional<String> candidateExplanation = aiExplanationGenerator.generate(context);
            if (candidateExplanation.isPresent() && validationService.isValid(candidateExplanation.get(), context)) {
                log.debug("[INVESTIGATION-SERVICE][EXPLANATION] Selected AI explanation for order {}", context.orderId());
                return new InvestigationExplanation(candidateExplanation, InvestigationExplanation.Source.AI);
            }

            log.warn("[INVESTIGATION-SERVICE][EXPLANATION] AI explanation validation failed for order {}; using fallback",
                    context.orderId());
        } catch (RuntimeException exception) {
            log.warn("[INVESTIGATION-SERVICE][EXPLANATION] AI explanation generation failed for order {}; using fallback",
                    context.orderId(), exception);
        }

        Optional<String> deterministic = deterministicGenerator.generate(context);
        if (deterministic.isPresent()) {
            log.debug("[INVESTIGATION-SERVICE][EXPLANATION] Selected deterministic explanation for order {}", context.orderId());
            return new InvestigationExplanation(deterministic, InvestigationExplanation.Source.DETERMINISTIC);
        }

        log.debug("[INVESTIGATION-SERVICE][EXPLANATION] No explanation is available for order {}", context.orderId());
        return InvestigationExplanation.unavailable();
    }
}
