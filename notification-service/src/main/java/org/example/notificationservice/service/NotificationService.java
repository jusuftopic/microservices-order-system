package org.example.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.contracts.NotificationRequestedEvent;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    private final JavaMailSender mailSender;


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

        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(event.recipientEmail());
            message.setSubject(buildSubject(event));
            message.setText(event.message());

            mailSender.send(message);

            log.info(
                    "[NOTIFICATION-SERVICE] Email successfully sent to {} for order {}",
                    event.recipientEmail(),
                    event.orderId()
            );

        } catch (Exception ex) {

            log.error(
                    "[NOTIFICATION-SERVICE] Failed to send email to {} for order {}. Error: {}",
                    event.recipientEmail(),
                    event.orderId(),
                    ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * Builds email subject based on notification type.
     *
     * @param event notification event
     * @return subject string
     */
    private String buildSubject(NotificationRequestedEvent event) {

        return switch (event.type()) {
            case "ORDER_COMPLETED" -> "Your order has been completed";
            case "ORDER_FAILED" -> "Your order has failed";
            case "ORDER_COMPENSATED" -> "Your order has been refunded";
            default -> "Order notification";
        };
    }

}
