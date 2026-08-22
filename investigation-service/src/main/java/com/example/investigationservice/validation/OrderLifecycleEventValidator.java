package com.example.investigationservice.validation;

import com.example.investigationservice.exception.InvalidLifecycleEventException;
import org.example.messagingstarter.contracts.events.OrderLifecycleTransitionedEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the structural contract required to store lifecycle evidence.
 * It deliberately does not decide whether a business status transition is
 * valid.
 */
@Component
public class OrderLifecycleEventValidator {

    private static final int SUPPORTED_EVENT_VERSION = 1;

    /**
     * Validates an event and its Kafka coordinates before persistence.
     *
     * @param event lifecycle event to validate
     * @param kafkaTopic source Kafka topic
     * @param kafkaPartition source Kafka partition
     * @param kafkaOffset source Kafka offset
     * @throws InvalidLifecycleEventException when required evidence is missing
     *         or exceeds the persistence contract
     */
    public void validate(
            OrderLifecycleTransitionedEvent event,
            String kafkaTopic,
            int kafkaPartition,
            long kafkaOffset
    ) {
        if (event == null) {
            throw new InvalidLifecycleEventException("Lifecycle event must not be null");
        }

        List<String> violations = new ArrayList<>();
        requirePositive(event.orderId(), "orderId", violations);
        requireText(event.previousStatus(), 64, "previousStatus", violations);
        requireText(event.newStatus(), 64, "newStatus", violations);
        requireText(event.reasonCode(), 128, "reasonCode", violations);
        requireText(event.sourceService(), 128, "sourceService", violations);
        requireText(event.sourceEventType(), 255, "sourceEventType", violations);
        requireText(event.correlationId(), 255, "correlationId", violations);
        requireText(kafkaTopic, 255, "kafkaTopic", violations);

        if (event.messageId() == null) {
            violations.add("messageId must not be null");
        }
        if (event.occurredAt() == null) {
            violations.add("occurredAt must not be null");
        }
        if (event.eventVersion() != SUPPORTED_EVENT_VERSION) {
            violations.add("eventVersion must be " + SUPPORTED_EVENT_VERSION);
        }
        if (kafkaPartition < 0) {
            violations.add("kafkaPartition must not be negative");
        }
        if (kafkaOffset < 0) {
            violations.add("kafkaOffset must not be negative");
        }

        validateDecision(event.orchestrationDecision(), violations);
        validateCompensation(event.compensation(), violations);

        if (!violations.isEmpty()) {
            throw new InvalidLifecycleEventException(
                    "Invalid lifecycle event: " + String.join(", ", violations)
            );
        }
    }

    private void validateDecision(
            OrderLifecycleTransitionedEvent.OrchestrationDecision decision,
            List<String> violations
    ) {
        if (decision == null) {
            return;
        }

        requireText(decision.code(), 128, "orchestrationDecision.code", violations);
        requireText(
                decision.targetService(),
                128,
                "orchestrationDecision.targetService",
                violations
        );
        if (decision.commandId() == null) {
            violations.add("orchestrationDecision.commandId must not be null");
        }
    }

    private void validateCompensation(
            OrderLifecycleTransitionedEvent.Compensation compensation,
            List<String> violations
    ) {
        if (compensation == null) {
            violations.add("compensation must not be null");
            return;
        }

        if (compensation.required()) {
            requireText(compensation.type(), 128, "compensation.type", violations);
        } else if (hasText(compensation.type())) {
            violations.add("compensation.type must be empty when compensation is not required");
        }
    }

    private void requirePositive(Long value, String field, List<String> violations) {
        if (value == null || value <= 0) {
            violations.add(field + " must be greater than zero");
        }
    }

    private void requireText(
            String value,
            int maximumLength,
            String field,
            List<String> violations
    ) {
        if (!hasText(value)) {
            violations.add(field + " must not be blank");
        } else if (value.length() > maximumLength) {
            violations.add(field + " must not exceed " + maximumLength + " characters");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
