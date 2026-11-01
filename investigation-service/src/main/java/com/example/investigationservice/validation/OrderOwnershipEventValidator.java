package com.example.investigationservice.validation;

import com.example.investigationservice.exception.InvalidOrderOwnershipEventException;
import org.example.messagingstarter.contracts.events.OrderOwnershipAssignedEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates ownership event data required by the local authorization projection.
 */
@Component
public class OrderOwnershipEventValidator {

    private static final int SUPPORTED_EVENT_VERSION = 1;

    /**
     * Validates an ownership event and its Kafka source coordinates.
     *
     * @param event ownership event to validate
     * @param kafkaTopic source Kafka topic
     * @param kafkaPartition source Kafka partition
     * @param kafkaOffset source Kafka offset
     * @throws InvalidOrderOwnershipEventException when required data is invalid
     */
    public void validate(
            OrderOwnershipAssignedEvent event,
            String kafkaTopic,
            int kafkaPartition,
            long kafkaOffset
    ) {
        if (event == null) {
            throw new InvalidOrderOwnershipEventException(
                    "Order ownership event must not be null"
            );
        }

        List<String> violations = new ArrayList<>();
        if (event.orderId() == null || event.orderId() <= 0) {
            violations.add("orderId must be greater than zero");
        }
        if (event.customerId() == null) {
            violations.add("customerId must not be null");
        }
        if (event.messageId() == null) {
            violations.add("messageId must not be null");
        }
        if (event.occurredAt() == null) {
            violations.add("occurredAt must not be null");
        }
        if (event.eventVersion() != SUPPORTED_EVENT_VERSION) {
            violations.add("eventVersion must be " + SUPPORTED_EVENT_VERSION);
        }
        requireText(event.correlationId(), "correlationId", violations);
        requireText(kafkaTopic, "kafkaTopic", violations);
        if (kafkaPartition < 0) {
            violations.add("kafkaPartition must not be negative");
        }
        if (kafkaOffset < 0) {
            violations.add("kafkaOffset must not be negative");
        }

        if (!violations.isEmpty()) {
            throw new InvalidOrderOwnershipEventException(
                    "Invalid order ownership event: " + String.join(", ", violations)
            );
        }
    }

    /**
     * Adds a violation when a persisted text value is missing or too long.
     *
     * @param value field value
     * @param field field name used in the diagnostic
     * @param violations accumulated validation failures
     */
    private void requireText(String value, String field, List<String> violations) {
        if (value == null || value.isBlank()) {
            violations.add(field + " must not be blank");
        } else if (value.length() > 255) {
            violations.add(field + " must not exceed 255 characters");
        }
    }
}
