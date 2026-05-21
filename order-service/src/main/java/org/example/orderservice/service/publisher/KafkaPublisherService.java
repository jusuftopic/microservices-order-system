package org.example.orderservice.service.publisher;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.utils.TopicResolver;
import org.example.orderservice.entity.OutboxEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


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

    private final KafkaTemplate<String, String> kafkaTemplate;


    /**
     * Publishes an outbox event to Kafka.
     *
     * @param event outbox event containing metadata and serialized payload
     * @return send result metadata
     */
    public CompletableFuture<SendResult<String, String>> publishEvent(OutboxEvent event) {

        String topic = TopicResolver.resolveTopic(event.getEventType());

        CompletableFuture<SendResult<String, String>> result = kafkaTemplate.send(
                topic,
                event.getAggregateId().toString(),
                event.getPayload()
        );

        log.info("[ORDER-SERVICE][KAFKA] Event {} published to topic {}",
                event.getEventType(), topic);

        return result;
    }

}
