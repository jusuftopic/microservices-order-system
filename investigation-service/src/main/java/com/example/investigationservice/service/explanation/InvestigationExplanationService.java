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

    private final Optional<AiExplanationGenerator> aiExplanationGenerator;
    private final ExplanationValidationService validationService;
    private final DeterministicExplanationGenerator deterministicGenerator;

    /**
     * Produces the explanation selected for an investigation.
     *
     * @param context authoritative investigation context
     * @return selected explanation and its source
     */
    public InvestigationExplanation explain(InvestigationContext context) {
        if (aiExplanationGenerator.isPresent()) {
            try {
                Optional<String> candidate = aiExplanationGenerator.get().generate(context);
                if (candidate.isPresent()
                        && validationService.isValid(candidate.get(), context)) {
                    log.debug("Selected AI explanation for order {}", context.orderId());
                    return new InvestigationExplanation(
                            candidate,
                            InvestigationExplanation.Source.AI
                    );
                }

                log.warn("AI explanation validation failed for order {}; using fallback",
                        context.orderId());
            } catch (RuntimeException exception) {
                log.warn("AI explanation generation failed for order {}; using fallback",
                        context.orderId(), exception);
            }
        } else {
            log.debug("No AI explanation adapter is configured for order {}; using fallback",
                    context.orderId());
        }

        Optional<String> deterministic = deterministicGenerator.generate(context);
        if (deterministic.isPresent()) {
            log.debug("Selected deterministic explanation for order {}", context.orderId());
            return new InvestigationExplanation(
                    deterministic,
                    InvestigationExplanation.Source.DETERMINISTIC
            );
        }

        log.debug("No explanation is available for order {}", context.orderId());
        return InvestigationExplanation.unavailable();
    }
}
