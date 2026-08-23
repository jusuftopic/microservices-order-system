package com.example.investigationservice.listener.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.kafka.KafkaConsumerReliabilitySupport;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Observes terminal lifecycle records rejected by Investigation Service.
 */
@Service
@Slf4j
public class InvestigationDlqKafkaListener {

    /**
     * Records dead-letter metadata without automatically replaying the record.
     *
     * @param record dead-letter record retained by Kafka
     */
    @KafkaListener(
            topics = EventConstants.TOPIC_INVESTIGATION_ORDER_LIFECYCLE_DLT,
            groupId = "${spring.kafka.consumer.group-id}-dlq",
            containerFactory = KafkaConsumerReliabilitySupport.DEAD_LETTER_LISTENER_FACTORY
    )
    public void handleDeadLetter(ConsumerRecord<String, Object> record) {
        log.warn(
                "[INVESTIGATION-SERVICE][KAFKA-DLQ] Observed terminal record {}-{}@{} valueType {}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.value() == null ? "null" : record.value().getClass().getName()
        );
    }
}
