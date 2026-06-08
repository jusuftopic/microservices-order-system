package org.example.paymentservice.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an outgoing event to be published to Kafka.
 *
 * <p>
 * Implements the Outbox Pattern for reliable message delivery.
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
    @NotNull
    @Column(name = "aggregate_type", length = 50)
    private String aggregateType;

    /**
     * Identifier of the aggregate instance that produced the event.
     */
    @NotNull
    @Column(name = "aggregate_id")
    private Long aggregateId;

    /**
     * Type of the event (e.g. "CREATED", "UPDATED", "DELETED").
     */
    @NotNull
    @Size(max = 50)
    @Column(name = "event_type", length = 50)
    private String eventType;

    /**
     * Serialized event payload (typically JSON).
     */
    @NotNull
    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    /**
     * Indicates whether the event has been processed
     * and published to the message broker.
     */
    @NotNull
    @Column(name = "processed", nullable = false)
    private Boolean processed;


    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    /**
     * Timestamp when the event was created.
     */
    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Initializes default values before persisting.
     */
    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (processed == null) {
            processed = false;
        }
    }

}
