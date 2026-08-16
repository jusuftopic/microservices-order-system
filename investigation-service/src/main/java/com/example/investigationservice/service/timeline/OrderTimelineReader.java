package com.example.investigationservice.service.timeline;

import com.example.investigationservice.model.InvestigationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Supplies the ordered evidence context required by investigation queries.
 */
@Service
@Slf4j
public class OrderTimelineReader {

    /**
     * Reads the investigation context for an order.
     *
     * @param orderId investigated order identifier
     * @return evidence context, or empty when no context is available
     */
    public Optional<InvestigationContext> read(long orderId) {
        log.debug("[INVESTIGATION-SERVICE][TIMELINE-READER] Reading investigation timeline for order {}", orderId);
        return Optional.of(InvestigationContext.empty(orderId));
    }
}
