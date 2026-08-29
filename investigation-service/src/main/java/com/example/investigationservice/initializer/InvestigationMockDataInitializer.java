package com.example.investigationservice.initializer;

import com.example.investigationservice.entity.OrderLifecycleEvidence;
import com.example.investigationservice.repository.OrderLifecycleEvidenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.lifecycle.CompensationType;
import org.example.messagingstarter.contracts.lifecycle.LifecycleReasonCode;
import org.example.messagingstarter.contracts.lifecycle.OrchestrationDecisionCode;
import org.example.messagingstarter.contracts.lifecycle.OrderStatus;
import org.example.messagingstarter.contracts.lifecycle.ServiceName;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Adds opt-in mock order timelines for local Investigation API testing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "app.mock-data.enabled",
        havingValue = "true"
)
public class InvestigationMockDataInitializer implements ApplicationRunner {

    static final long COMPLETED_ORDER_ID = 9001L;
    static final long FAILED_ORDER_ID = 9002L;

    private static final String MOCK_CORRELATION_PREFIX = "mock-order-";
    private static final Instant BASE_TIME = Instant.parse("2026-08-01T10:00:00Z");

    private final OrderLifecycleEvidenceRepository repository;

    /**
     * Persists missing mock evidence without modifying existing timelines.
     *
     * @param args application startup arguments
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<OrderLifecycleEvidence> missingEvidence = mockEvidence().stream()
                .filter(evidence -> !repository.existsByMessageId(evidence.getMessageId()))
                .toList();

        if (missingEvidence.isEmpty()) {
            log.info("[INVESTIGATION-SERVICE][MOCK-DATA] Mock timelines already exist");
            return;
        }

        repository.saveAll(missingEvidence);
        log.info(
                "[INVESTIGATION-SERVICE][MOCK-DATA] Added {} lifecycle entries for demo orders {} and {}",
                missingEvidence.size(),
                COMPLETED_ORDER_ID,
                FAILED_ORDER_ID
        );
    }

    private List<OrderLifecycleEvidence> mockEvidence() {
        return List.of(
                evidence(
                        "00000000-0000-0000-0000-000000009001",
                        COMPLETED_ORDER_ID,
                        OrderStatus.CREATED,
                        OrderStatus.INVENTORY_RESERVE_COMPLETED,
                        LifecycleReasonCode.INVENTORY_RESERVED,
                        ServiceName.INVENTORY_SERVICE,
                        EventConstants.EVENT_INVENTORY_RESERVED,
                        OrchestrationDecisionCode.PROCESS_PAYMENT,
                        null,
                        BASE_TIME,
                        0L
                ),
                evidence(
                        "00000000-0000-0000-0000-000000009002",
                        COMPLETED_ORDER_ID,
                        OrderStatus.INVENTORY_RESERVE_COMPLETED,
                        OrderStatus.PAYMENT_COMPLETED,
                        LifecycleReasonCode.PAYMENT_COMPLETED,
                        ServiceName.PAYMENT_SERVICE,
                        EventConstants.EVENT_PAYMENT_SUCCESS,
                        OrchestrationDecisionCode.COMMIT_INVENTORY,
                        null,
                        BASE_TIME.plusSeconds(30),
                        1L
                ),
                evidence(
                        "00000000-0000-0000-0000-000000009003",
                        COMPLETED_ORDER_ID,
                        OrderStatus.PAYMENT_COMPLETED,
                        OrderStatus.INVENTORY_COMMIT_COMPLETED,
                        LifecycleReasonCode.INVENTORY_COMMITTED,
                        ServiceName.INVENTORY_SERVICE,
                        EventConstants.EVENT_INVENTORY_COMMIT_COMPLETED,
                        null,
                        null,
                        BASE_TIME.plusSeconds(60),
                        2L
                ),
                evidence(
                        "00000000-0000-0000-0000-000000009004",
                        COMPLETED_ORDER_ID,
                        OrderStatus.INVENTORY_COMMIT_COMPLETED,
                        OrderStatus.COMPLETED,
                        LifecycleReasonCode.ORDER_WORKFLOW_COMPLETED,
                        ServiceName.INVENTORY_SERVICE,
                        EventConstants.EVENT_INVENTORY_COMMIT_COMPLETED,
                        OrchestrationDecisionCode.SEND_NOTIFICATION,
                        null,
                        BASE_TIME.plusSeconds(90),
                        3L
                ),
                evidence(
                        "00000000-0000-0000-0000-000000009005",
                        FAILED_ORDER_ID,
                        OrderStatus.INVENTORY_RESERVE_COMPLETED,
                        OrderStatus.PAYMENT_FAILED,
                        LifecycleReasonCode.PAYMENT_FAILED,
                        ServiceName.PAYMENT_SERVICE,
                        EventConstants.EVENT_PAYMENT_FAILED,
                        OrchestrationDecisionCode.RELEASE_INVENTORY,
                        CompensationType.INVENTORY_RELEASE,
                        BASE_TIME.plusSeconds(120),
                        4L
                ),
                evidence(
                        "00000000-0000-0000-0000-000000009006",
                        FAILED_ORDER_ID,
                        OrderStatus.PAYMENT_FAILED,
                        OrderStatus.FAILED,
                        LifecycleReasonCode.COMPENSATION_COMPLETED,
                        ServiceName.INVENTORY_SERVICE,
                        EventConstants.EVENT_INVENTORY_RELEASE_COMPLETED,
                        OrchestrationDecisionCode.SEND_NOTIFICATION,
                        null,
                        BASE_TIME.plusSeconds(150),
                        5L
                )
        );
    }

    private OrderLifecycleEvidence evidence(
            String messageId,
            long orderId,
            OrderStatus previousStatus,
            OrderStatus newStatus,
            LifecycleReasonCode reasonCode,
            ServiceName sourceService,
            String sourceEventType,
            OrchestrationDecisionCode decision,
            CompensationType compensationType,
            Instant occurredAt,
            long kafkaOffset
    ) {
        UUID commandId = decision == null
                ? null
                : UUID.nameUUIDFromBytes((messageId + "-command").getBytes(UTF_8));

        return OrderLifecycleEvidence.builder()
                .messageId(UUID.fromString(messageId))
                .orderId(orderId)
                .previousStatus(previousStatus.code())
                .newStatus(newStatus.code())
                .reasonCode(reasonCode.code())
                .sourceService(sourceService.code())
                .sourceEventType(sourceEventType)
                .decisionCode(decision == null ? null : decision.code())
                .decisionTargetService(
                        decision == null ? null : decision.targetService().code()
                )
                .decisionCommandId(commandId)
                .compensationRequired(compensationType != null)
                .compensationType(
                        compensationType == null ? null : compensationType.code()
                )
                .occurredAt(occurredAt)
                .eventVersion(1)
                .correlationId(MOCK_CORRELATION_PREFIX + orderId)
                .kafkaTopic(EventConstants.TOPIC_ORDER_LIFECYCLE_V1)
                .kafkaPartition(0)
                .kafkaOffset(kafkaOffset)
                .build();
    }
}
