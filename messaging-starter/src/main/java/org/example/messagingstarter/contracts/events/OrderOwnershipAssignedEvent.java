package org.example.messagingstarter.contracts.events;

import org.example.messagingstarter.contracts.BaseEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Fact emitted when the Order Service assigns an order to its customer owner.
 *
 * @param orderId unique order identifier
 * @param customerId opaque internal identifier of the customer that owns the order
 * @param occurredAt time at which ownership was assigned
 * @param eventVersion contract version
 * @param correlationId identifier used to trace the order workflow
 * @param messageId stable identifier used for message deduplication
 */
public record OrderOwnershipAssignedEvent(
        Long orderId,
        UUID customerId,
        Instant occurredAt,
        int eventVersion,
        String correlationId,
        UUID messageId
) implements BaseEvent {
}
