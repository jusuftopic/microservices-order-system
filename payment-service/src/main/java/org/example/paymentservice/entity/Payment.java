package org.example.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.paymentservice.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment entity represents a payment attempt for an Order.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    /**
     * Unique payment identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to Order.
     */
    @Column(nullable = false, unique = true)
    private Long orderId;

    /**
     * Payment state.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /**
     * Payment provider (e.g., STRIPE, PAYPAL).
     */
    @Column
    private String provider;

    /**
     * External transaction ID.
     */
    private String transactionId;

    /**
     * Stable identifier used for every provider request belonging to this
     * payment operation.
     */
    @Column(nullable = false, unique = true, updatable = false)
    private UUID providerIdempotencyKey;

    /**
     * Correlation ID.
     */
    private String correlationId;

    /**
     * Payment failure reason
     */
    private String failureReason;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
