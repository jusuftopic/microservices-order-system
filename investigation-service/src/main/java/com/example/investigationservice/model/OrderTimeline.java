package com.example.investigationservice.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Stable, chronologically ordered lifecycle evidence for one order.
 *
 * @param orderId investigated order identifier
 * @param entries lifecycle evidence ordered from oldest to newest
 */
public record OrderTimeline(
        long orderId,
        List<OrderTimelineEntry> entries
) {

    public OrderTimeline {
        if (orderId <= 0) {
            throw new IllegalArgumentException("orderId must be greater than zero");
        }
        entries = List.copyOf(Objects.requireNonNull(entries, "entries must not be null"));
    }

    /**
     * Creates a timeline without collected evidence.
     *
     * @param orderId investigated order identifier
     * @return empty order timeline
     */
    public static OrderTimeline empty(long orderId) {
        return new OrderTimeline(orderId, List.of());
    }

    /**
     * Indicates whether lifecycle evidence is available.
     *
     * @return {@code true} when the timeline contains at least one entry
     */
    public boolean hasEvidence() {
        return !entries.isEmpty();
    }

    /**
     * Returns the latest authoritative status from the ordered timeline.
     *
     * @return latest status, when evidence is available
     */
    public Optional<String> currentStatus() {
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(entries.get(entries.size() - 1).newStatus());
    }
}
