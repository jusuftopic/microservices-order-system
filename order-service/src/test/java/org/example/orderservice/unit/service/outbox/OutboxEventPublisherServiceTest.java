package org.example.orderservice.unit.service.outbox;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.TimeoutException;
import org.example.commons.event.EventConstants;
import org.example.messagingstarter.outbox.entity.OutboxEvent;
import org.example.messagingstarter.outbox.repository.OutboxRepository;
import org.example.messagingstarter.outbox.service.OutboxDlqService;
import org.example.orderservice.service.kafka.KafkaPublisherService;
import org.example.orderservice.service.outbox.OutboxEventPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxEventPublisherServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxDlqService dlqService;

    @Mock
    private KafkaPublisherService kafkaPublisherService;

    private OutboxEventPublisherService service;

    @BeforeEach
    public void setUp() {
        service = new OutboxEventPublisherService(
                outboxRepository, dlqService, kafkaPublisherService
        );
    }

    @Test
    void should_do_nothing_when_no_pending_events() {

        // GIVEN
        when(outboxRepository.findReadyForPublishing(any()))
                .thenReturn(List.of());

        // WHEN
        service.publishPendingEvents();

        // THEN
        verifyNoInteractions(kafkaPublisherService);
        verify(outboxRepository).findReadyForPublishing(any());
    }

    @Test
    void should_publish_event_and_mark_processed() throws Exception {

        // GIVEN
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(1L);
        event.setPayload("payload");
        event.setRetryCount(0);
        event.setEventType(EventConstants.EVENT_INVENTORY_CHECK_REQUESTED);

        when(outboxRepository.findReadyForPublishing(any()))
                .thenReturn(List.of(event));

        // simulate Kafka success
        RecordMetadata metadata = mock(RecordMetadata.class);

        SendResult<String, Object> sendResult = mock(SendResult.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);

        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(sendResult);

        when(kafkaPublisherService.publishEvent(event))
                .thenReturn(future);

        // WHEN
        service.publishPendingEvents();

        // THEN
        assertTrue(event.getProcessed());
        assertEquals(1, event.getRetryCount());
        assertNotNull(event.getLastAttemptAt());

        verify(outboxRepository, atLeastOnce()).save(event);
        verify(dlqService, never()).storeOutboxDlq(
                any(),
                anyLong(),
                anyString(),
                any(),
                anyInt(),
                any()
        );
    }

    @Test
    void should_retry_when_kafka_fails_and_not_reach_max_retries() throws Exception {

        // GIVEN
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(1L);
        event.setPayload("payload");
        event.setRetryCount(0);
        event.setEventType(EventConstants.EVENT_INVENTORY_CHECK_REQUESTED);


        when(outboxRepository.findReadyForPublishing(any()))
                .thenReturn(List.of(event));

        // simulate Kafka failure
        CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(new TimeoutException("Kafka down"));

        when(kafkaPublisherService.publishEvent(event))
                .thenReturn(future);

        // WHEN
        service.publishPendingEvents();

        // THEN
        assertFalse(event.getProcessed());
        assertEquals(1, event.getRetryCount());

        verify(outboxRepository).save(event);
        verify(dlqService, never()).storeOutboxDlq(
                any(),
                anyLong(),
                anyString(),
                any(),
                anyInt(),
                any()
        );
    }

    @Test
    void should_process_multiple_events() {

        // GIVEN
        OutboxEvent e1 = new OutboxEvent();
        e1.setId(UUID.randomUUID());
        e1.setAggregateId(1L);
        e1.setPayload("p1");
        e1.setEventType(EventConstants.EVENT_INVENTORY_CHECK_REQUESTED);

        OutboxEvent e2 = new OutboxEvent();
        e2.setId(UUID.randomUUID());
        e2.setAggregateId(2L);
        e2.setPayload("p2");
        e2.setEventType(EventConstants.EVENT_INVENTORY_CHECK_REQUESTED);

        when(outboxRepository.findReadyForPublishing(any()))
                .thenReturn(List.of(e1, e2));
        // WHEN
        service.publishPendingEvents();

        // THEN
        verify(kafkaPublisherService).publishEvent(e1);
        verify(kafkaPublisherService).publishEvent(e2);

        verify(outboxRepository, atLeast(2)).save(any());
    }
}
