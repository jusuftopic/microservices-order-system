package com.example.investigationservice.dto.response;

import java.util.List;
import java.util.Objects;

/**
 * Investigation result exposed for a single order.
 *
 * @param orderId       investigated order identifier
 * @param dataAvailable whether timeline evidence is currently available
 * @param currentStatus latest authoritative order status, when available
 * @param explanation   human-readable explanation, when available
 * @param timeline      ordered lifecycle evidence for the order
 */
public record OrderInvestigationResponse(
        long orderId,
        boolean dataAvailable,
        String currentStatus,
        String explanation,
        List<OrderTimelineEntryResponse> timeline
) {

    public OrderInvestigationResponse {
        timeline = List.copyOf(Objects.requireNonNull(timeline, "timeline must not be null"));
    }

    /**
     * Creates a valid response for an order with no projected evidence.
     *
     * @param orderId investigated order identifier
     * @return response containing no status, explanation or timeline entries
     */
    public static OrderInvestigationResponse empty(long orderId) {
        return new OrderInvestigationResponse(orderId, false, null, null, List.of());
    }
}
