package org.example.notificationservice.listener.kafka;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.NotificationRequestedEvent;
import org.example.notificationservice.service.NotificationService;
import org.example.notificationservice.utils.Constants;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka listener responsible for handling incoming notification requests.
 *
 */

@Service
@RequiredArgsConstructor
@Slf4j
@KafkaListener(
        topics = EventConstants.TOPIC_NOTIFICATION_REQUEST_V1,
        groupId = Constants.KAFKA_NOTIFICATION_GROUP_ID
)
public class NotificationRequestKafkaListener {

    private final NotificationService notificationService;

    /**
     * Handles incoming notification request events.
     *
     * @param event notification request event
     */
    @KafkaHandler
    public void handleNotificationRequested(
            NotificationRequestedEvent event
    ) {

        log.info(
                "[NOTIFICATION-SERVICE][KAFKA] Received NotificationRequestedEvent for order {} type {} recipient {} correlationId {}",
                event.orderId(),
                event.type(),
                event.recipientEmail(),
                event.correlationId()
        );

        notificationService.processNotification(event);
    }

    /**
     * Catch-all fallback method to capture unmapped data shapes safely
     * without breaking consumers.
     *
     * @param unknownMessage unknown message
     */
    @KafkaHandler(isDefault = true)
    public void handleUnknownObject(Object unknownMessage) {

        log.error(
                "[NOTIFICATION-SERVICE][KAFKA] Unmatched notification event signature fallback triggered! Payload object type: {}",
                unknownMessage.getClass().getName()
        );
    }

}
