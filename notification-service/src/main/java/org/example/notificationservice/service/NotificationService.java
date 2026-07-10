package org.example.notificationservice.service;

import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.contracts.NotificationRequestedEvent;
import org.example.notificationservice.metrics.NotificationMetrics;
import org.example.notificationservice.service.sender.NotificationSender;
import org.springframework.stereotype.Service;


/**
 * Service responsible for processing and delivering user notifications.
 *
 * <p>
 * This service acts as the core component of the Notification Service.
 * It receives notification requests from Kafka listeners and performs
 * best-effort delivery of messages (e.g. sending emails).
 * </p>
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationService {

    private final NotificationSender notificationSender;
    private final NotificationMetrics notificationMetrics;

    /**
     * Processes incoming notification event and sends email.
     *
     * <p>
     * This method performs a best-effort email delivery.
     * Failures are logged but do not propagate, ensuring that
     * notification handling does not affect upstream services.
     * </p>
     *
     * @param event notification request event
     */
    public void processNotification(NotificationRequestedEvent event) {

        log.info(
                "[NOTIFICATION-SERVICE] Processing notification for order {} type {} recipient {}",
                event.orderId(),
                event.type(),
                event.recipientEmail()
        );
        incrementMetrics(notificationMetrics.getNotificationRequestsTotal());

        try {
            notificationSender.send(event);
            incrementMetrics(notificationMetrics.getNotificationSentTotal());
        } catch (Exception ex) {
            log.error(
                    "[NOTIFICATION-SERVICE] Unexpected error during notification handling. order={} recipient={} error={}",
                    event.orderId(),
                    event.recipientEmail(),
                    ex.getMessage(),
                    ex
            );

            incrementMetrics(notificationMetrics.getNotificationFailedTotal());
        }
    }

    private void incrementMetrics(final Counter counter) {
        if (counter != null) counter.increment();
    }
}
