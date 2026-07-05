package org.example.messagingstarter.outbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OutboxEvent represents a persisted domain event used in the
 * Outbox Pattern for reliable event publishing.
 *
 * <p>
 * Each record stores an event that will later be processed
 * and published to an external Kafka messaging system.
 * </p>
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    /**
     * Unique identifier of the outbox event.
     */
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * Type of the aggregate that produced the event
     * (e.g. "USER", "ORDER").
     */
    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    /**
     * Identifier of the aggregate instance that produced the event.
     */
    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    /**
     * Type of the event (e.g. "CREATED", "UPDATED", "DELETED").
     */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /**
     * Serialized event payload (typically JSON).
     */
    @Lob
    @Column(nullable = false)
    private String payload;

    /**
     * Indicates whether the event has been processed
     * and published to the message broker.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean processed = false;

    /**
     * Counter how many times event retries
     */
    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    /**
     * Calculated next retry schedule time including backoff
     */
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;


    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {

        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
