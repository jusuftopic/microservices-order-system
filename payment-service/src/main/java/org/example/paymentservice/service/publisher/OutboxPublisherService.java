package org.example.paymentservice.service.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.utils.Constants;
import org.example.paymentservice.entity.OutboxEvent;
import org.example.paymentservice.repository.OutboxRepository;
import org.example.paymentservice.service.OutboxDlqService;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Publishes Outbox events to Kafka reliably.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherService {

    private final OutboxRepository repository;
    private final OutboxDlqService outboxDlqService;
    private final KafkaPublisherService kafkaPublisherService;

    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = repository.findPendingEvents();

        if (events == null || events.isEmpty()) {
            log.debug("[PAYMENT-SERVICE][OUTBOX-PUBLISHER] No pending events found.");
            return;
        }

        events.forEach(this::publishSingleEvent);
    }

    private void publishSingleEvent(final OutboxEvent event) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastAttemptAt(LocalDateTime.now());
        try {
            SendResult<String, Object> result = kafkaPublisherService.publishEvent(event).get();
            handleSuccess(event, result);
        }
        catch (Exception e) {
            handleFailure(event, e);
        }
    }

    private void handleSuccess(OutboxEvent event,
                               SendResult<String, Object> result) {
        event.setProcessed(true);
        repository.save(event);

        log.debug("[PAYMENT-SERVICE][OUTBOX-PUBLISHER] Event id={} type={} successfully published." +
                        "Topic {}",
                event.getId(), event.getEventType(),
                Optional.ofNullable(result)
                        .map(r -> r.getRecordMetadata() != null ? r.getRecordMetadata().topic() : null)
                        .orElse(null));
    }

    private void handleFailure(OutboxEvent event, Throwable ex) {
        log.warn("[PAYMENT-SERVICE][OUTBOX-PUBLISHER] Failed event {} retry {}",
                event.getId(), event.getRetryCount(), ex);

        if (event.getRetryCount() >= Constants.MAX_RETRIES_KAFKA) {
            /* store to DLQ table */
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
            repository.save(event);

            log.warn("[PAYMENT-SERVICE][OUTBOX-PUBLISHER] Event {} moved to DLQ table after {} retries.",
                    event.getId(), event.getRetryCount());
        } else {
            repository.save(event);
        }
    }
}
