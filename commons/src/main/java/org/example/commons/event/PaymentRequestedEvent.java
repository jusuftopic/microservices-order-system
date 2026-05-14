package org.example.commons.event;

import java.util.UUID;

/**
 * Event indicates payment request after successful order creation
 */
public record PaymentRequestedEvent(
        UUID eventId,
        Long orderId
) {
}
