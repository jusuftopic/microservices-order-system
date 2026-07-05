package org.example.messagingstarter.outbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.*;
import org.example.messagingstarter.outbox.entity.OutboxEvent;
import org.example.messagingstarter.outbox.repository.OutboxRepository;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private final OutboxDlqService outboxDlqService;
    private final EventPublisherService eventPublisherService;

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

        List<OutboxEvent> events = outboxRepository.findReadyForPublishing(LocalDateTime.now());

        if (events == null || events.isEmpty()) {
            log.debug("[MESSAGING-STARTER][OUTBOX-PUBLISHER] No pending events found.");
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
            SendResult<String, Object> result = eventPublisherService.publishEvent(event).get();
            handleSuccess(event, result);
        }
        catch (Exception e) {
            handleFailure(event, e);
        }
    }


    private void handleSuccess(OutboxEvent event,
                               SendResult<String, Object> result) {
        event.setProcessed(true);
        outboxRepository.save(event);
        final String topic = Optional.ofNullable(result.getRecordMetadata())
                        .map(RecordMetadata::topic).orElse(null);

        log.debug("[MESSAGING-STARTER][OUTBOX-PUBLISHER] Event id={} type={} successfully published." +
                        "Topic {}",
                event.getId(), event.getEventType(), topic);
    }

    private void handleFailure(OutboxEvent event, Throwable ex) {
        if (isRetryable(ex)) {
            handleRetryableFailure(event, ex);
        } else {
            handleNonRetryableFailure(event, ex);
        }
    }

    private void handleRetryableFailure(OutboxEvent event, Throwable ex) {
        Duration backoff = calculateBackoff(event.getRetryCount());
        event.setNextRetryAt(LocalDateTime.now().plus(backoff));

        outboxRepository.save(event);

        log.warn(
                "[MESSAGING-STARTER][OUTBOX-PUBLISHER] Retryable failure for event {}. Retry {} scheduled at {}",
                event.getId(),
                event.getRetryCount(),
                event.getNextRetryAt(),
                ex
        );
    }

    private void handleNonRetryableFailure(OutboxEvent event, Throwable ex)
    {
        log.error("[MESSAGING-STARTER][OUTBOX-PUBLISHER] Non-retryable failure for event {}", event.getId(), ex);
        moveToDlq(event, ex);
    }

    private void moveToDlq(OutboxEvent event, Throwable ex) {
        outboxDlqService.storeOutboxDlq(
                event.getId(),
                event.getAggregateId(),
                event.getEventType(),
                event.getPayload(),
                event.getRetryCount(),
                ex
        );

        /* mark event as processed in Outbox table */
        event.setProcessed(true);
        outboxRepository.save(event);

        log.warn("[MESSAGING-STARTER][OUTBOX-PUBLISHER] Event {} moved to DLQ table after {} retries.",
                event.getId(), event.getRetryCount());
    }

    private boolean isRetryable(Throwable ex) {
        Throwable cause = rootCause(ex);

        return
                // timeout due to network congestion or broker load
                cause instanceof TimeoutException
                // Indicates the connection to the broker was lost mid-flight.
                || cause instanceof DisconnectException
                // Transient replication issue or leader changes
                || cause instanceof NotEnoughReplicasException
                || cause instanceof NotEnoughReplicasAfterAppendException
                //  Occurs during partition re-election
                || cause instanceof UnknownLeaderEpochException
                //  The partition leader is in flux.
                || cause instanceof LeaderNotAvailableException
                // The consumer group coordinator is initializing or currently unavailable.
                || cause instanceof CoordinatorNotAvailableException
                || cause instanceof CoordinatorLoadInProgressException
                // The consumer is not communicating with the current coordinator or is undergoing a partition rebalance.
                || cause instanceof NotCoordinatorException
                || cause instanceof ReassignmentInProgressException
                // A checksum error where a retry may succeed.
                || cause instanceof CorruptRecordException
                // A retriable error caused by exceeding client quotas.
                || cause instanceof ThrottlingQuotaExceededException;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable cause = throwable;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause;
    }

    public Duration calculateBackoff(int retryCount) {

        return switch (retryCount) {

            case 1 -> Duration.ofSeconds(3);

            case 2 -> Duration.ofSeconds(10);

            case 3 -> Duration.ofSeconds(30);

            case 4 -> Duration.ofMinutes(1);

            case 5 -> Duration.ofMinutes(5);

            default -> Duration.ofMinutes(15);
        };
    }

}
