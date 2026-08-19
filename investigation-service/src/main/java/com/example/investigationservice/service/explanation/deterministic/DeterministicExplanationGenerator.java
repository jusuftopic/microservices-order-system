package com.example.investigationservice.service.explanation.deterministic;

import com.example.investigationservice.model.InvestigationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Produces evidence-based explanations without an external model provider.
 */
@Service
@Slf4j
public class DeterministicExplanationGenerator {

    /**
     * Generates a deterministic explanation from collected evidence.
     *
     * @param context authoritative investigation context
     * @return deterministic explanation, when one can be produced
     */
    public Optional<String> generate(InvestigationContext context) {
        log.debug("[INVESTIGATION-SERVICE][DETERMINISTIC-EXPLANATION] Generating deterministic explanation for order {}", context.orderId());
        return Optional.empty();
    }
}
