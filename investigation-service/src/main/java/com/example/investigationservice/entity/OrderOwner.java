package com.example.investigationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
 * Immutable local authorization projection for the customer that owns an order.
 */
@Entity
@Table(
        name = "order_owners",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_owners_message_id",
                columnNames = "message_id"
        )
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Immutable
public class OrderOwner {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "message_id", nullable = false, updatable = false)
    private UUID messageId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "event_version", nullable = false, updatable = false)
    private int eventVersion;

    @Column(name = "correlation_id", nullable = false, updatable = false)
    private String correlationId;

    /**
     * Records when the ownership fact reached this service.
     */
    @PrePersist
    void initializeReceivedAt() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}
