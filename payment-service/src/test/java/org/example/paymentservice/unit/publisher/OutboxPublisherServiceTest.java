package org.example.paymentservice.unit.publisher;

import org.example.commons.event.EventConstants;
import org.example.commons.event.utils.Constants;
import org.example.paymentservice.entity.OutboxEvent;
import org.example.paymentservice.repository.OutboxRepository;
import org.example.paymentservice.service.OutboxDlqService;
import org.example.paymentservice.service.publisher.KafkaPublisherService;
import org.example.paymentservice.service.publisher.OutboxPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxPublisherServiceTest {

    @Mock
    private OutboxRepository repository;

    @Mock
    private OutboxDlqService outboxDlqService;

    @Mock
    private KafkaPublisherService kafkaPublisherService;

    @Mock
    private SendResult<String, Object> sendResult;

    private OutboxPublisherService service;


    @BeforeEach
    void setUp() {
        service = new OutboxPublisherService(
                repository,
                outboxDlqService,
                kafkaPublisherService
        );
    }


    @Test
    void should_publish_pending_events_successfully() throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_PAYMENT_SUCCESS
        );

        when(repository.findPendingEvents())
                .thenReturn(List.of(event));

        when(kafkaPublisherService.publishEvent(event))
                .thenReturn(
                        CompletableFuture.completedFuture(sendResult)
                );

        // WHEN
        service.publishPendingEvents();

        // THEN
        assertTrue(event.getProcessed());
        assertEquals(1, event.getRetryCount());
        assertNotNull(event.getLastAttemptAt());

        verify(repository).findPendingEvents();
        verify(kafkaPublisherService).publishEvent(event);
        verify(repository).save(event);

        verifyNoInteractions(outboxDlqService);
    }


    @Test
    void should_do_nothing_when_no_pending_events_exist() {

        // GIVEN
        when(repository.findPendingEvents())
                .thenReturn(Collections.emptyList());

        // WHEN
        service.publishPendingEvents();

        // THEN
        verify(repository).findPendingEvents();

        verifyNoInteractions(
                kafkaPublisherService,
                outboxDlqService
        );

        verify(repository, never()).save(any());
    }


    @Test
    void should_retry_and_not_send_to_dlq_when_retry_count_below_limit()
            throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_PAYMENT_FAILED
        );

        event.setRetryCount(0);

        when(repository.findPendingEvents())
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> failedFuture =
                new CompletableFuture<>();

        failedFuture.completeExceptionally(
                new RuntimeException("Kafka unavailable")
        );

        when(kafkaPublisherService.publishEvent(event))
                .thenReturn(failedFuture);

        // WHEN
        service.publishPendingEvents();

        // THEN
        assertFalse(event.getProcessed());
        assertEquals(1, event.getRetryCount());
        assertNotNull(event.getLastAttemptAt());

        verify(repository).save(event);

        verifyNoInteractions(outboxDlqService);
    }


    @Test
    void should_move_event_to_dlq_when_retry_limit_reached()
            throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_PAYMENT_FAILED
        );

        event.setRetryCount(Constants.MAX_RETRIES_KAFKA - 1);

        when(repository.findPendingEvents())
                .thenReturn(List.of(event));

        CompletableFuture<SendResult<String, Object>> failedFuture =
                new CompletableFuture<>();

        RuntimeException exception =
                new RuntimeException("Broker down");

        failedFuture.completeExceptionally(exception);

        when(kafkaPublisherService.publishEvent(event))
                .thenReturn(failedFuture);

        // WHEN
        service.publishPendingEvents();

        // THEN
        assertTrue(event.getProcessed());
        assertEquals(Constants.MAX_RETRIES_KAFKA, event.getRetryCount());

        verify(outboxDlqService).storeOutboxDlq(
                eq(event.getId()),
                eq(event.getAggregateId()),
                eq(event.getEventType()),
                eq(event.getPayload()),
                eq(event.getRetryCount()),
                any(Throwable.class)
        );

        verify(repository).save(event);
    }

    @Test
    void should_increment_retry_count_before_publish_attempt()
            throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_PAYMENT_SUCCESS
        );

        event.setRetryCount(5);

        when(repository.findPendingEvents())
                .thenReturn(List.of(event));

        when(kafkaPublisherService.publishEvent(event))
                .thenReturn(
                        CompletableFuture.completedFuture(sendResult)
                );

        // WHEN
        service.publishPendingEvents();

        // THEN
        assertEquals(6, event.getRetryCount());
        assertNotNull(event.getLastAttemptAt());

        verify(repository).save(event);
    }

    @Test
    void should_publish_multiple_events() throws Exception {

        // GIVEN
        OutboxEvent first = createEvent(
                EventConstants.EVENT_PAYMENT_SUCCESS
        );

        OutboxEvent second = createEvent(
                EventConstants.EVENT_PAYMENT_FAILED
        );

        when(repository.findPendingEvents())
                .thenReturn(List.of(first, second));

        when(kafkaPublisherService.publishEvent(any()))
                .thenReturn(
                        CompletableFuture.completedFuture(sendResult)
                );

        // WHEN
        service.publishPendingEvents();

        // THEN
        verify(kafkaPublisherService).publishEvent(first);
        verify(kafkaPublisherService).publishEvent(second);

        verify(repository).save(first);
        verify(repository).save(second);

        assertTrue(first.getProcessed());
        assertTrue(second.getProcessed());
    }

    private OutboxEvent createEvent(String eventType) {

        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("PAYMENT")
                .aggregateId(1L)
                .eventType(eventType)
                .payload("{json}")
                .processed(false)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }


}
