package com.example.investigationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable evidence of an authoritative order lifecycle transition.
 */
@Entity
@Table(
        name = "order_lifecycle_evidence",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_lifecycle_evidence_message_id",
                columnNames = "message_id"
        ),
        indexes = @Index(
                name = "idx_order_lifecycle_evidence_timeline",
                columnList = "order_id, occurred_at, message_id"
        )
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
public class OrderLifecycleEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, updatable = false)
    private UUID messageId;

    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Column(name = "previous_status", nullable = false, updatable = false, length = 64)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, updatable = false, length = 64)
    private String newStatus;

    @Column(name = "reason_code", nullable = false, updatable = false, length = 128)
    private String reasonCode;

    @Column(name = "source_service", nullable = false, updatable = false, length = 128)
    private String sourceService;

    @Column(name = "source_event_type", nullable = false, updatable = false)
    private String sourceEventType;

    @Column(name = "causation_id", updatable = false)
    private UUID causationId;

    @Column(name = "decision_code", updatable = false, length = 128)
    private String decisionCode;

    @Column(name = "decision_target_service", updatable = false, length = 128)
    private String decisionTargetService;

    @Column(name = "decision_command_id", updatable = false)
    private UUID decisionCommandId;

    @Column(name = "compensation_required", nullable = false, updatable = false)
    private boolean compensationRequired;

    @Column(name = "compensation_type", updatable = false, length = 128)
    private String compensationType;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "event_version", nullable = false, updatable = false)
    private int eventVersion;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private String correlationId;

    @Column(name = "kafka_topic", nullable = false, updatable = false)
    private String kafkaTopic;

    @Column(name = "kafka_partition", nullable = false, updatable = false)
    private int kafkaPartition;

    @Column(name = "kafka_offset", nullable = false, updatable = false)
    private long kafkaOffset;

    @PrePersist
    void initializeReceivedAt() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}
