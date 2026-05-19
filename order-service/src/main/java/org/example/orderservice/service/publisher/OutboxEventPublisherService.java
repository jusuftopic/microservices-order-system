package org.example.orderservice.service.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.utils.TopicResolver;
import org.example.orderservice.entity.OutboxDlqEvent;
import org.example.orderservice.entity.OutboxEvent;
import org.example.orderservice.repository.OutboxDlqRepository;
import org.example.orderservice.repository.OutboxRepository;
import org.example.orderservice.utils.Constants;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service responsible for publishing Outbox Events to Kafka.
 *
 * <p>
 * Implements the Outbox Pattern:
 * <ul>
 *     <li>Reads unprocessed events from DB</li>
 *     <li>Publishes them to Kafka</li>
 *     <li>Marks them as processed</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisherService {

    private final OutboxRepository outboxRepository;
    private final OutboxDlqRepository dlqRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Publishes all pending outbox events.
     *
     * <p>
     * Each event is published to Kafka and marked as processed only
     * if publishing succeeds.
     * </p>
     */
    @Transactional
    public void publishPendingEvents() {

        List<OutboxEvent> events = outboxRepository.findByProcessedFalseOrderByCreatedAtAsc();
        if (events == null || events.isEmpty()) {
            log.debug("[ORDER-SERVICE][OUTBOX-PUBLISHER] No pending events found.");
            return;
        }

        events.forEach(this::publishSingleEvent);
    }

    /**
     * Publishes a single outbox event to Kafka and marks it as processed.
     *
     * <p>
     * If publishing fails, the event remains unprocessed and will be retried
     * in the next scheduled execution.
     * </p>
     */
    private void publishSingleEvent(OutboxEvent event) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastAttemptAt(LocalDateTime.now());

        try {
            String topic = TopicResolver.resolveTopic(event.getEventType());
            SendResult<String, String> result = kafkaTemplate.send(
                    topic,
                    event.getAggregateId().toString(),
                    event.getPayload()
            ).get();

            handleSuccess(event, result);
        }
        catch (Exception e) {
            handleFailure(event, e);
        }
    }


    private void handleSuccess(OutboxEvent event,
                               SendResult<String, String> result) {
        event.setProcessed(true);
        outboxRepository.save(event);

        log.debug("[ORDER-SERVICE][OUTBOX-PUBLISHER] Event id={} type={} successfully published." +
                        "Topic {}",
                event.getId(), event.getEventType(), result.getRecordMetadata().topic());
    }

    private void handleFailure(OutboxEvent event, Throwable ex) {
        log.error("[ORDER-SERVICE][OUTBOX-PUBLISHER] Failed event {} retry {}",
                event.getId(), event.getRetryCount(), ex);
        if (event.getRetryCount() >= Constants.MAX_RETRIES) {
            moveToDlq(event, ex);
        } else {
            outboxRepository.save(event);
        }
    }

    private void moveToDlq(OutboxEvent event, Throwable ex) {
        final OutboxDlqEvent dlqEvent = new OutboxDlqEvent();
        dlqEvent.setOriginalEventId(event.getId());
        dlqEvent.setAggregateId(event.getAggregateId());
        dlqEvent.setEventType(event.getEventType());
        dlqEvent.setPayload(event.getPayload());
        dlqEvent.setErrorMessage(ex.getMessage());
        dlqEvent.setRetryCount(event.getRetryCount());

        /* persist in DLQ table */
        dlqRepository.save(dlqEvent);

        /* mark event as processed in Outbox table */
        event.setProcessed(true);
        outboxRepository.save(event);

        log.warn("[ORDER-SERVICE][OUTBOX-PUBLISHER] Event {} moved to DLQ table after {} retries.",
                event.getId(), event.getRetryCount());
    }
}
