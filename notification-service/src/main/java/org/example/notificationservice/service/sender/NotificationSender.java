package org.example.notificationservice.service.sender;

import org.example.commons.event.contracts.NotificationRequestedEvent;

/**
 * Abstraction for sending notifications.
 *
 * <p>
 * This interface decouples the NotificationService from specific delivery mechanisms
 * (e.g. email, SMS, push notification).
 * </p>
 *
 * <p>
 * Different implementations can provide actual delivery or act as placeholders
 * (e.g. NoOp for local/dev environments).
 * </p>
 */
public interface NotificationSender {


    /**
     * Sends a notification based on the provided event.
     *
     * <p>
     * Implementations should handle delivery in a best-effort manner
     * and may choose to swallow exceptions or propagate them depending
     * on their responsibility.
     * </p>
     *
     * @param event notification request event
     */
    void send(NotificationRequestedEvent event);

}
