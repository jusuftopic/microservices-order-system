package org.example.messagingstarter.outbox.service;

import org.example.messagingstarter.outbox.entity.OutboxEvent;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;


/**
 * Publishes Outbox events to the underlying messaging infrastructure.
 *
 * <p>Implemented by each microservice (e.g. Kafka) while the messaging
 * framework remains broker-agnostic.</p>
 */
public interface EventPublisherService {


    /**
     * Publishes the given outbox event.
     *
     * @param event event to publish
     * @return future completed when publication succeeds or fails
     */
    CompletableFuture<SendResult<String, Object>> publishEvent(OutboxEvent event);

}
