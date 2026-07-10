package org.example.messagingstarter.contracts;


import java.util.UUID;

/**
 * Base contract for all events in the system.
 *
 * <p>
 * Provides common fields required for message-level idempotency
 * and workflow tracing across services.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *     <li><b>orderId</b> - order id </li>
 *     <li><b>messageId</b> - unique identifier of the message (used for Inbox deduplication)</li>
 *     <li><b>correlationId</b> - identifier used to trace the full workflow (Saga)</li>
 * </ul>
 * </p>
 */
public interface BaseEvent {

    /**
     * Unique identifier of the order.
     */
    Long orderId();

    /**
     * Unique identifier of the message.
     */
    UUID messageId();

    /**
     * Correlation identifier used for tracking the workflow across services.
     */
    String correlationId();

}
