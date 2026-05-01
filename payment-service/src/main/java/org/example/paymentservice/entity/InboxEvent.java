package org.example.paymentservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * InboxEvent represents a processed event in the Inbox Pattern.
 *
 * <p>
 * The Inbox Pattern ensures idempotent processing of incoming events
 * by storing already processed event IDs.
 * </p>
 *
 * <p>
 * If an event exists in this table, it has already been handled and
 * should NOT be processed again.
 * </p>
 */
@Entity
@Table(name = "inbox_event")
@Getter
@Setter
public class InboxEvent {

    /**
     * Unique identifier of the incoming event.
     */
    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    /**
     * Timestamp when the event was successfully processed.
     */
    @NotNull
    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    /**
     * Sets the processing timestamp automatically before insert.
     */
    @PrePersist
    public void prePersist() {
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }

}
