package com.example.investigationservice.service;

import com.example.investigationservice.entity.OrderLifecycleEvidence;
import com.example.investigationservice.repository.OrderLifecycleEvidenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.contracts.events.OrderLifecycleTransitionedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maps order lifecycle events to the Investigation Service's evidence model
 * and persists them as timeline records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderLifecycleEvidencePersistenceService {

    private final OrderLifecycleEvidenceRepository repository;

    /**
     * Persists a lifecycle event together with the Kafka coordinates from
     * which it was consumed. Repeated deliveries of an already stored message
     * are ignored.
     *
     * @param event lifecycle event to store
     * @param kafkaTopic topic from which the event was consumed
     * @param kafkaPartition partition from which the event was consumed
     * @param kafkaOffset offset at which the event was consumed
     * @return {@code true} when new evidence was stored, or {@code false} when
     *         the message had already been persisted
     */
    @Transactional
    public boolean persist(
            OrderLifecycleTransitionedEvent event,
            String kafkaTopic,
            int kafkaPartition,
            long kafkaOffset
    ) {
        if (repository.existsByMessageId(event.messageId())) {
            log.debug(
                    "Lifecycle evidence already exists for messageId {}; skipping duplicate delivery",
                    event.messageId()
            );
            return false;
        }

        repository.save(toEvidence(event, kafkaTopic, kafkaPartition, kafkaOffset));
        log.debug(
                "Persisted lifecycle evidence for order {} with messageId {} from {}-{}@{}",
                event.orderId(),
                event.messageId(),
                kafkaTopic,
                kafkaPartition,
                kafkaOffset
        );
        return true;
    }

    private OrderLifecycleEvidence toEvidence(
            OrderLifecycleTransitionedEvent event,
            String kafkaTopic,
            int kafkaPartition,
            long kafkaOffset
    ) {
        OrderLifecycleTransitionedEvent.OrchestrationDecision decision =
                event.orchestrationDecision();
        OrderLifecycleTransitionedEvent.Compensation compensation =
                event.compensation();

        return OrderLifecycleEvidence.builder()
                .messageId(event.messageId())
                .orderId(event.orderId())
                .previousStatus(event.previousStatus())
                .newStatus(event.newStatus())
                .reasonCode(event.reasonCode())
                .sourceService(event.sourceService())
                .sourceEventType(event.sourceEventType())
                .causationId(event.causationId())
                .decisionCode(decision == null ? null : decision.code())
                .decisionTargetService(decision == null ? null : decision.targetService())
                .decisionCommandId(decision == null ? null : decision.commandId())
                .compensationRequired(compensation != null && compensation.required())
                .compensationType(compensation == null ? null : compensation.type())
                .occurredAt(event.occurredAt())
                .eventVersion(event.eventVersion())
                .correlationId(event.correlationId())
                .kafkaTopic(kafkaTopic)
                .kafkaPartition(kafkaPartition)
                .kafkaOffset(kafkaOffset)
                .build();
    }
}
