package org.example.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a notification to be delivered to a user.
 *
 * <p>
 * This entity is used by the Notification Service to persist
 * outgoing messages (e.g. email notifications).
 * It is intentionally lightweight to avoid tight coupling with
 * upstream services and to support best-effort delivery.
 * </p>
 *
 * <p>
 * Each notification corresponds to a business event (e.g. order completed,
 * payment failed, inventory released) and contains all necessary data
 * to construct an email message.
 * </p>
 */

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    /**
     * Unique identifier of the notification.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Email address of the recipient.
     */
    private String recipientEmail;

    /**
     * Subject of the notification (used as email subject).
     */
    private String subject;

    /**
     * Message content of the notification (email body).
     */
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * Type of notification (e.g. ORDER_COMPLETED, ORDER_FAILED).
     */
    private String type;

    /**
     * Timestamp when the notification was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when the notification was sent.
     */
    private LocalDateTime sentAt;

    /**
     * Correlation identifier used for tracing the workflow across services.
     */
    private String correlationId;


}
