package org.example.paymentservice.service.publisher;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.PaymentCompletedEvent;
import org.example.commons.event.contracts.PaymentFailedEvent;
import org.example.commons.event.utils.TopicResolver;
import org.example.messagingstarter.outbox.entity.OutboxEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for publishing payment-related events to Kafka.
 *
 * <p>
 * This class encapsulates all Kafka communication logic for the Payment Service.
 * It ensures that topic usage is centralized and prevents scattering
 * Kafka-related code across business services.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Publish event payment success</li>
 *     <li>Publish event payment failed</li>
 * </ul>
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

        log.info("[PAYMENT-SERVICE][KAFKA] Sending event {} to the topic {}",
                event.getEventType(), topic);

        CompletableFuture<SendResult<String, Object>> result = kafkaTemplate.send(
                topic,
                event.getAggregateId().toString(),
                deserialize(event)
        );

        log.info("[PAYMENT-SERVICE][KAFKA] Event {} published to topic {}",
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

                case EventConstants.EVENT_PAYMENT_SUCCESS ->
                        objectMapper.readValue(
                                event.getPayload(),
                                PaymentCompletedEvent.class
                        );

                case EventConstants.EVENT_PAYMENT_FAILED ->
                        objectMapper.readValue(
                                event.getPayload(),
                                PaymentFailedEvent.class
                        );

                default -> null;
            };

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to deserialize outbox payload for event " + event.getEventType(),
                    ex
            );
        }
    }
}
