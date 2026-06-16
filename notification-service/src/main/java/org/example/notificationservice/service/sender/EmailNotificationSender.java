package org.example.notificationservice.service.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.contracts.NotificationRequestedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email-based implementation of {@link NotificationSender}.
 *
 * <p>
 * This implementation uses {@link JavaMailSender} to send emails.
 * It performs best-effort delivery and logs any failures without
 * propagating exceptions.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.email.enabled", havingValue = "true")
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;

    @Override

    public void send(NotificationRequestedEvent event) {

        log.info(
                "[NOTIFICATION-SERVICE][EMAIL-SENDER] Sending email: order={} type={} recipient={}",
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
                    "[NOTIFICATION-SERVICE][EMAIL-SENDER] Email sent to {} for order {}",
                    event.recipientEmail(),
                    event.orderId()
            );

        } catch (Exception ex) {

            log.error(
                    "[NOTIFICATION-SERVICE][EMAIL-SENDER] Failed to send email to {} for order {}. Error: {}",
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
