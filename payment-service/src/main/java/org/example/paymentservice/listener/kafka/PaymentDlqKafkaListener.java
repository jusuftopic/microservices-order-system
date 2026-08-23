package org.example.paymentservice.listener.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.kafka.KafkaConsumerReliabilitySupport;
import org.example.paymentservice.utils.Constants;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Observes terminal records rejected by Payment Service consumers.
 */
@Service
public class PaymentDlqKafkaListener {

    /**
     * Records dead-letter metadata without automatically replaying the record.
     *
     * @param record dead-letter record retained by Kafka
     */
    @KafkaListener(
            topics = EventConstants.TOPIC_PAYMENT_DLQ,
            groupId = Constants.KAFKA_PAYMENT_DLQ_GROUP_ID,
            containerFactory = KafkaConsumerReliabilitySupport.DEAD_LETTER_LISTENER_FACTORY
    )
    public void handlePaymentRequestedDLT(ConsumerRecord<String, byte[]> record) {
        KafkaConsumerReliabilitySupport.logTerminalDeadLetter("PAYMENT-SERVICE", record);
    }
}
