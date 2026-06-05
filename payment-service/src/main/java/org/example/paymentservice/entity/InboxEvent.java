package org.example.paymentservice.entity;

import jakarta.persistence.*;

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

    @Id
    @Column(nullable = false, updatable = false, name = "message_id")
    private UUID messageId;

    /**
     * Timestamp of when the message was processed.
     */
    private LocalDateTime processedAt;

    /**
     * Initializes default values before persisting.
     */
    @PrePersist
    public void prePersist() {
        processedAt = LocalDateTime.now();
    }

}
