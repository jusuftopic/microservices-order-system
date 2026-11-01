package com.example.investigationservice.validation;

import com.example.investigationservice.exception.InvalidOrderOwnershipEventException;
import org.example.messagingstarter.contracts.events.OrderOwnershipAssignedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Verifies the contract and Kafka-coordinate rules for ownership events.
 */
class OrderOwnershipEventValidatorTest {

    private static final UUID CUSTOMER_ID =
            UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID MESSAGE_ID =
            UUID.fromString("2b997737-7918-442b-bbd8-c26e52fce083");

    private final OrderOwnershipEventValidator validator =
            new OrderOwnershipEventValidator();

    /**
     * Verifies that a complete version-one event is accepted.
     */
    @Test
    void acceptsValidOwnershipEvent() {
        assertDoesNotThrow(() -> validator.validate(
                validEvent("correlation-42"),
                "order.ownership.v1",
                0,
                7L
        ));
    }

    /**
     * Verifies the explicit diagnostic for a missing event object.
     */
    @Test
    void rejectsNullEvent() {
        assertThatThrownBy(() -> validator.validate(null, "order.ownership.v1", 0, 7L))
                .isInstanceOf(InvalidOrderOwnershipEventException.class)
                .hasMessage("Order ownership event must not be null");
    }

    /**
     * Verifies all required event fields and Kafka coordinates in one malformed event.
     */
    @Test
    void reportsAllMissingAndInvalidValues() {
        OrderOwnershipAssignedEvent invalid = new OrderOwnershipAssignedEvent(
                0L,
                null,
                null,
                2,
                " ",
                null
        );

        assertThatThrownBy(() -> validator.validate(invalid, "", -1, -1L))
                .isInstanceOf(InvalidOrderOwnershipEventException.class)
                .hasMessageContaining("orderId must be greater than zero")
                .hasMessageContaining("customerId must not be null")
                .hasMessageContaining("messageId must not be null")
                .hasMessageContaining("occurredAt must not be null")
                .hasMessageContaining("eventVersion must be 1")
                .hasMessageContaining("correlationId must not be blank")
                .hasMessageContaining("kafkaTopic must not be blank")
                .hasMessageContaining("kafkaPartition must not be negative")
                .hasMessageContaining("kafkaOffset must not be negative");
    }

    /**
     * Verifies the database-safe length limit for persisted text fields.
     */
    @Test
    void rejectsOversizedTextValues() {
        String oversized = "x".repeat(256);

        assertThatThrownBy(() -> validator.validate(
                validEvent(oversized),
                oversized,
                0,
                7L
        ))
                .isInstanceOf(InvalidOrderOwnershipEventException.class)
                .hasMessageContaining("correlationId must not exceed 255 characters")
                .hasMessageContaining("kafkaTopic must not exceed 255 characters");
    }

    /**
     * Creates a structurally valid ownership event.
     *
     * @param correlationId correlation identifier to test
     * @return ownership event fixture
     */
    private OrderOwnershipAssignedEvent validEvent(String correlationId) {
        return new OrderOwnershipAssignedEvent(
                42L,
                CUSTOMER_ID,
                Instant.parse("2026-10-27T10:15:00Z"),
                1,
                correlationId,
                MESSAGE_ID
        );
    }
}
