package org.example.paymentservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_dlq")
@Getter
@Setter
public class OutboxDlqEvent {

    @Id
    @GeneratedValue
    private Long id;

    private UUID originalEventId;

    private Long aggregateId;

    private String eventType;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;

    private String errorMessage;

    private int retryCount;

    private LocalDateTime failedAt;

    @PrePersist
    public void prePersist() {
        if (failedAt == null) {
            failedAt = LocalDateTime.now();
        }
    }
}
