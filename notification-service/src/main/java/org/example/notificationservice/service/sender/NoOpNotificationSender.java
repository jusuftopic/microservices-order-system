package org.example.notificationservice.service.sender;


import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.contracts.commands.SendNotificationCommand;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * No-operation implementation of {@link NotificationSender}.
 *
 * <p>
 * This implementation does not perform any real delivery.
 * It simply logs that the notification would have been sent.
 * </p>
 *
 * <p>
 * Useful for:
 * <ul>
 *     <li>Local development</li>
 *     <li>Testing environments</li>
 *     <li>Fallback when no integration is configured</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@Primary
public class NoOpNotificationSender implements NotificationSender {


    @Override
    public void send(SendNotificationCommand event) {

        log.info(
                "[NOTIFICATION-SERVICE][NO-OP] Pretending to send notification: order={} type={} recipient={}",
                event.orderId(),
                event.type(),
                event.recipientEmail()
        );

    }
}
