package org.example.messagingstarter.contracts;


import java.util.UUID;

/**
 * Event emitted to request creation of a user notification.
 *
 * <p>
 * This event represents a choreography-based communication mechanism
 * used by services (e.g. Order Service) to trigger notifications
 * without tightly coupling to the Notification Service.
 * </p>
 *
 * <p>
 * It contains all information required to generate
 * an end-user message (e.g. email).
 * </p>
 *
 * <p>
 * Consumed by Notification Service.
 * </p>
 */
public record NotificationRequestedEvent(

        /**
        * Unique identifier of the order.
        */
        Long orderId,

        /**
         * Email address of the recipient.
         */
        String recipientEmail,

         /**
         * Type of notification (e.g. ORDER_COMPLETED, ORDER_FAILED).
         */
        String type,

         /**
         * Human-readable message content.
         */
        String message,

         /**
         * Correlation identifier used for tracking the workflow across services.
         */
        String correlationId,


        /**
         * Unique identifier of the message.
         */
        UUID messageId
) implements BaseEvent {
}
