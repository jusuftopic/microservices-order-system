package org.example.orderservice.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity storing processed messages for idempotency.
 *
 * <p>
 * Ensures that the same Kafka message (identified by messageId)
 * is processed only once.
 * </p>
 */
@Entity
@Table(name = "inbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
