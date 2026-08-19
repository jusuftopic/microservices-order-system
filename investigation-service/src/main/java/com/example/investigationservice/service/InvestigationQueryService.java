package com.example.investigationservice.service;

import com.example.investigationservice.dto.response.OrderInvestigationResponse;
import com.example.investigationservice.model.InvestigationContext;
import com.example.investigationservice.model.InvestigationExplanation;
import com.example.investigationservice.service.explanation.InvestigationExplanationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Application service responsible for assembling order investigation reports.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvestigationQueryService {

    private final OrderTimelineReaderService timelineReader;
    private final InvestigationExplanationService explanationService;

    /**
     * Builds the investigation report for an order.
     *
     * @param orderId positive order identifier
     * @return investigation report for the order
     */
    public OrderInvestigationResponse getOrderInvestigation(long orderId) {
        if (orderId <= 0) {
            throw new IllegalArgumentException("orderId must be greater than zero");
        }

        log.debug("[INVESTIGATION-SERVICE][QUERY-SERVICE] Building investigation response for order {}", orderId);

        InvestigationContext context = timelineReader.read(orderId)
                .orElseGet(() -> {
                    log.warn("[INVESTIGATION-SERVICE][QUERY-SERVICE] No investigation context returned for order {}; using empty context",
                            orderId);
                    return InvestigationContext.empty(orderId);
                });
        InvestigationExplanation explanation = explanationService.explain(context);

        log.debug("[INVESTIGATION-SERVICE][QUERY-SERVICE] Built investigation response for order {} with {} evidence items and explanation source {}",
                orderId, context.evidence().size(), explanation.source());

        return new OrderInvestigationResponse(
                orderId,
                context.hasEvidence(),
                context.currentStatus(),
                explanation.text().orElse(null),
                java.util.List.of()
        );
    }
}
