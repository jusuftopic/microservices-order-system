package org.example.inventoryservice.unit.outbox;

import org.example.commons.event.utils.Constants;
import org.example.messagingstarter.outbox.service.OutboxDlqService;
import org.example.inventoryservice.service.kafka.KafkaPublisherService;
import org.example.inventoryservice.service.outbox.OutboxPublisherService;
import org.example.messagingstarter.outbox.entity.OutboxEvent;
import org.example.messagingstarter.outbox.repository.OutboxRepository;
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

/**
 * Tests {@link OutboxPublisherService}
 */
@ExtendWith(MockitoExtension.class)
public class OutboxPublisherServiceTest {

    @Mock
    private OutboxRepository repository;

    @Mock
    private OutboxDlqService outboxDlqService;

    @Mock
    private KafkaPublisherService kafkaPublisherService;

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
    void should_do_nothing_when_no_events() {

        // GIVEN
        when(repository.findReadyForPublishing(any())).thenReturn(Collections.emptyList());

        // WHEN
        service.publishPendingEvents();

        // THEN
        verify(repository).findReadyForPublishing(any());
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(kafkaPublisherService);
    }


    @Test
    void should_publish_event_successfully_and_mark_processed() throws Exception {

        // GIVEN
        OutboxEvent event = createEvent();

        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(repository.findReadyForPublishing(any()))
                .thenReturn(List.of(event));

        when(kafkaPublisherService.publishEvent(event))
                .thenReturn(future);

        // WHEN
        service.publishPendingEvents();

        // THEN
        assertTrue(event.getRetryCount() > 0);
        assertNotNull(event.getLastAttemptAt());
        assertTrue(event.getProcessed());

        verify(repository).save(event);
        verify(outboxDlqService, never()).storeOutboxDlq(any(), any(), any(), any(), anyInt(), any());
    }


    @Test
    void should_retry_when_kafka_fails_below_threshold() {

        // GIVEN
        OutboxEvent event = createEvent();
        event.setRetryCount(0);

        when(repository.findReadyForPublishing(any()))
                .thenReturn(List.of(event));

        when(kafkaPublisherService.publishEvent(event))
                .thenReturn(failedFuture());

        // WHEN
        service.publishPendingEvents();

        // THEN
        assertEquals(1, event.getRetryCount());
        assertFalse(event.getProcessed());

        verify(repository).save(event);
        verify(outboxDlqService, never()).storeOutboxDlq(any(), any(), any(), any(), anyInt(), any());
    }


    @Test
    void should_move_to_dlq_when_retry_limit_exceeded() {

        // GIVEN
        OutboxEvent event = createEvent();
        event.setRetryCount(Constants.MAX_RETRIES_KAFKA);

        when(repository.findReadyForPublishing(any()))
                .thenReturn(List.of(event));

        when(kafkaPublisherService.publishEvent(event))
                .thenReturn(failedFuture());

        // WHEN
        service.publishPendingEvents();

        // THEN
        assertTrue(event.getProcessed());

        verify(outboxDlqService).storeOutboxDlq(
                eq(event.getId()),
                eq(event.getAggregateId()),
                eq(event.getEventType()),
                eq(event.getPayload()),
                eq(event.getRetryCount()),
                any()
        );

        verify(repository).save(event);
    }


    @Test
    void should_increment_retry_and_set_timestamp_on_attempt() {

        // GIVEN
        OutboxEvent event = createEvent();
        int initialRetry = event.getRetryCount();

        when(repository.findReadyForPublishing(any()))
                .thenReturn(List.of(event));

        when(kafkaPublisherService.publishEvent(event))
                .thenReturn(failedFuture());

        // WHEN
        service.publishPendingEvents();

        // THEN
        assertEquals(initialRetry + 1, event.getRetryCount());
        assertNotNull(event.getLastAttemptAt());
    }

    private OutboxEvent createEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(1L);
        event.setEventType("TEST_EVENT");
        event.setPayload("{json}");
        event.setProcessed(false);
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }

    private CompletableFuture<SendResult<String, Object>> failedFuture() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka failure"));
        return future;
    }

}
