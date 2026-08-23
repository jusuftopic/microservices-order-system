package org.example.inventoryservice.listener.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.kafka.KafkaConsumerReliabilitySupport;
import org.example.inventoryservice.utils.Constants;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Observes terminal records rejected by Inventory Service consumers.
 */
@Service
public class InventoryDlqKafkaListener {

    /**
     * Observes a terminal inventory dead-letter record without replaying it.
     *
     * @param record dead-letter record retained by Kafka
     */
    @KafkaListener(
            topics = EventConstants.TOPIC_INVENTORY_DLQ,
            groupId = Constants.KAFKA_INVENTORY_DLQ_GROUP_ID,
            containerFactory = KafkaConsumerReliabilitySupport.DEAD_LETTER_LISTENER_FACTORY
    )
    public void handleDltMessage(ConsumerRecord<String, byte[]> record) {
        KafkaConsumerReliabilitySupport.logTerminalDeadLetter("INVENTORY-SERVICE", record);
    }
}
