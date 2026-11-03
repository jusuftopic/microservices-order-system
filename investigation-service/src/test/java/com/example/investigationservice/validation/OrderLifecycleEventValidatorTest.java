package com.example.investigationservice.validation;

import com.example.investigationservice.exception.InvalidLifecycleEventException;

import org.example.messagingstarter.contracts.events.OrderLifecycleTransitionedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class OrderLifecycleEventValidatorTest {

    private OrderLifecycleEventValidator target;

    @BeforeEach
    public void setUp() {
        target = new OrderLifecycleEventValidator();
    }

    @Test
    public void throwExceptionWhenEventNull() {
        // WHEN
        assertThatThrownBy(() -> target.validate(null, "", 0, 0))
                .isInstanceOf(InvalidLifecycleEventException.class)
                .hasMessage("Lifecycle event must not be null");
    }

    @Test
    public void verifyValidLifecycleEvent() {
        // GIVEN
        UUID messageId = UUID.randomUUID();
        UUID causationId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        OrderLifecycleTransitionedEvent event = new OrderLifecycleTransitionedEvent(
                42L,
                "INVENTORY_RESERVE_COMPLETED",
                "PAYMENT_FAILED",
                "PAYMENT_FAILED",
                "PAYMENT_SERVICE",
                "PAYMENT_FAILED",
                causationId,
                new OrderLifecycleTransitionedEvent.OrchestrationDecision(
                        "RELEASE_INVENTORY",
                        "INVENTORY_SERVICE",
                        commandId
                ),
                new OrderLifecycleTransitionedEvent.Compensation(
                        true,
                        "INVENTORY_RELEASE"
                ),
                Instant.parse("2026-08-15T10:15:30Z"),
                1,
                "correlation-42",
                messageId
        );

        // WHEN
        assertDoesNotThrow(
                () -> target.validate(event, "test-topic-1", 1, 2)
        );
    }

    @Test
    public void throwExceptionWhenOrderIdNegativeNumber() {
        // GIVEN
        UUID messageId = UUID.randomUUID();
        UUID causationId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        OrderLifecycleTransitionedEvent event = new OrderLifecycleTransitionedEvent(
                -1L,
                "INVENTORY_RESERVE_COMPLETED",
                "PAYMENT_FAILED",
                "PAYMENT_FAILED",
                "PAYMENT_SERVICE",
                "PAYMENT_FAILED",
                causationId,
                new OrderLifecycleTransitionedEvent.OrchestrationDecision(
                        "RELEASE_INVENTORY",
                        "INVENTORY_SERVICE",
                        commandId
                ),
                new OrderLifecycleTransitionedEvent.Compensation(
                        true,
                        "INVENTORY_RELEASE"
                ),
                Instant.parse("2026-08-15T10:15:30Z"),
                1,
                "correlation-42",
                messageId
        );

        // WHEN
        assertThatThrownBy(() -> target.validate(event, "test-topic-1", 0, 0))
                .isInstanceOf(InvalidLifecycleEventException.class);
    }

    @Test
    public void throwExceptionWhenPreviousStatusEmpty() {
        // GIVEN
        UUID messageId = UUID.randomUUID();
        UUID causationId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        OrderLifecycleTransitionedEvent event = new OrderLifecycleTransitionedEvent(
                55L,
                "",
                "PAYMENT_FAILED",
                "PAYMENT_FAILED",
                "PAYMENT_SERVICE",
                "PAYMENT_FAILED",
                causationId,
                new OrderLifecycleTransitionedEvent.OrchestrationDecision(
                        "RELEASE_INVENTORY",
                        "INVENTORY_SERVICE",
                        commandId
                ),
                new OrderLifecycleTransitionedEvent.Compensation(
                        true,
                        "INVENTORY_RELEASE"
                ),
                Instant.parse("2026-08-15T10:15:30Z"),
                1,
                "correlation-42",
                messageId
        );

        // WHEN
        assertThatThrownBy(() -> target.validate(event, "test-topic-1", 0, 0))
                .isInstanceOf(InvalidLifecycleEventException.class);
    }
}
