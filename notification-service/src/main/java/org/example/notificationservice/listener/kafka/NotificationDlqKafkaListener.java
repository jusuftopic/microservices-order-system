package org.example.notificationservice.listener.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.kafka.KafkaConsumerReliabilitySupport;
import org.example.notificationservice.utils.Constants;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Observes terminal records rejected by Notification Service consumers.
 */
@Service
public class NotificationDlqKafkaListener {

    /**
     * Records dead-letter metadata without automatically replaying the record.
     *
     * @param record dead-letter record retained by Kafka
     */
    @KafkaListener(
            topics = EventConstants.TOPIC_NOTIFICATION_DLQ,
            groupId = Constants.KAFKA_NOTIFICATION_DLQ_GROUP_ID,
            containerFactory = KafkaConsumerReliabilitySupport.DEAD_LETTER_LISTENER_FACTORY
    )
    public void handleDeadLetter(ConsumerRecord<String, byte[]> record) {
        KafkaConsumerReliabilitySupport.logTerminalDeadLetter("NOTIFICATION-SERVICE", record);
    }
}
