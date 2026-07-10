package org.example.inventoryservice.listener.kafka;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.EventConstants;
import org.example.inventoryservice.utils.Constants;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@KafkaListener(
        topics = EventConstants.TOPIC_INVENTORY_DLQ,
        groupId = Constants.KAFKA_INVENTORY_GROUP_ID
)
public class InventoryDlqKafkaListener {

    /**
     * Handles messages from Dead Letter Topic.
     *
     * @param message raw message payload
     */
    @KafkaHandler
    public void handleDltMessage(String message) {

        log.warn(
                "[INVENTORY-SERVICE][KAFKA-DLT] Received message in DLT: {}",
                message
        );
    }

    /**
     * Catch-all fallback method.
     *
     * @param unknownMessage unknown message
     */
    @KafkaHandler(isDefault = true)
    public void handleUnknownObject(Object unknownMessage) {

        log.error(
                "[INVENTORY-SERVICE][KAFKA-DLT] Unknown message type received in DLT: {}",
                unknownMessage.getClass().getName()
        );
    }

}
