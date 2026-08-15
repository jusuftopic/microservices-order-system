package com.example.investigationservice.listener.kafka;

import com.example.investigationservice.service.OrderLifecycleEvidencePersistenceService;
import org.example.messagingstarter.contracts.events.OrderLifecycleTransitionedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OrderLifecycleTransitionKafkaListenerTest {

    private final OrderLifecycleEvidencePersistenceService persistenceService =
            mock(OrderLifecycleEvidencePersistenceService.class);
    private final OrderLifecycleTransitionKafkaListener listener =
            new OrderLifecycleTransitionKafkaListener(persistenceService);

    @Test
    void acceptsLifecycleTransition() {
        OrderLifecycleTransitionedEvent event = new OrderLifecycleTransitionedEvent(
                42L,
                "PAYMENT_COMPLETED",
                "PROCESSING",
                "PAYMENT_SUCCEEDED",
                "order-service",
                "PaymentCompletedEvent",
                UUID.randomUUID(),
                new OrderLifecycleTransitionedEvent.OrchestrationDecision(
                        "RESERVE_INVENTORY",
                        "inventory-service",
                        UUID.randomUUID()
                ),
                OrderLifecycleTransitionedEvent.Compensation.notRequired(),
                Instant.parse("2026-08-11T10:15:30Z"),
                1,
                "correlation-42",
                UUID.randomUUID()
        );

        listener.handleOrderLifecycleTransitioned(event, "order.lifecycle.v1", 0, 7L);

        verify(persistenceService).persist(event, "order.lifecycle.v1", 0, 7L);
    }

    @Test
    void safelyAcceptsUnknownPayload() {
        assertDoesNotThrow(() -> listener.handleUnknownObject("unexpected-event"));
    }
}
