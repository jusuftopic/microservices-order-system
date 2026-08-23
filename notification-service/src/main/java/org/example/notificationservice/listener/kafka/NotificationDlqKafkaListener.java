package org.example.notificationservice.listener.kafka;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    public void handleDeadLetter(ConsumerRecord<String, Object> record) {
        log.warn(
                "[NOTIFICATION-SERVICE][KAFKA-DLQ] Observed terminal record {}-{}@{} valueType {}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.value() == null ? "null" : record.value().getClass().getName()
        );
    }
}
