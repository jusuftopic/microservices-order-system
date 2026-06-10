package org.example.orderservice.service.kafka;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryRequestedEvent;
import org.example.commons.event.contracts.PaymentRequestedEvent;
import org.example.commons.event.utils.TopicResolver;
import org.example.orderservice.entity.OutboxEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;


/**
 * Service responsible for publishing order-related events to Kafka.
 *
 * <p>
 * This class centralizes all Kafka communication logic for the Order Service.
 * It ensures that topic resolution and message publishing are managed
 * in a single place.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publishes an outbox event to Kafka.
     *
     * @param event outbox event containing metadata and serialized payload
     * @return send result metadata
     */
    public CompletableFuture<SendResult<String, Object>> publishEvent(OutboxEvent event) {

        String topic = TopicResolver.resolveTopic(event.getEventType());

        CompletableFuture<SendResult<String, Object>> result = kafkaTemplate.send(
                topic,
                event.getAggregateId().toString(),
                deserialize(event)
        );

        log.debug("[ORDER-SERVICE][KAFKA] Event {} published to topic {}",
                event.getEventType(), topic);

        return result;
    }


    /**
     * Converts JSON payload to corresponding event object.
     *
     * @param event outbox event
     * @return deserialized payload object
     */
    private Object deserialize(OutboxEvent event) {

        try {
            return switch (event.getEventType()) {

                case EventConstants.EVENT_INVENTORY_CHECK_REQUESTED ->
                        objectMapper.readValue(
                                event.getPayload(),
                                InventoryRequestedEvent.class
                        );

                case EventConstants.EVENT_PAYMENT_REQUESTED ->
                        objectMapper.readValue(
                                event.getPayload(),
                                PaymentRequestedEvent.class
                        );

                default -> throw new IllegalArgumentException(
                        "Unsupported event type: " + event.getEventType()
                );
            };

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to deserialize outbox payload for event " + event.getEventType(),
                    ex
            );
        }
    }


}
