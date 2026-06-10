package org.example.orderservice.unit.service.outbox;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.example.commons.event.EventConstants;
import org.example.commons.event.utils.Constants;
import org.example.orderservice.entity.OutboxDlqEvent;
import org.example.orderservice.entity.OutboxEvent;
import org.example.orderservice.repository.OutboxDlqRepository;
import org.example.orderservice.repository.OutboxRepository;
import org.example.orderservice.service.kafka.KafkaPublisherService;
import org.example.orderservice.service.outbox.OutboxEventPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OutboxEventPublisherServiceTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxDlqRepository dlqRepository;

    @Mock
    private KafkaPublisherService kafkaPublisherService;

    private OutboxEventPublisherService service;

    @BeforeEach
    public void setUp() {
        service = new OutboxEventPublisherService(
                outboxRepository, dlqRepository, kafkaPublisherService
        );
    }

    @Test
    void should_do_nothing_when_no_pending_events() {

        // GIVEN
        when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of());

        // WHEN
        service.publishPendingEvents();

        // THEN
        verifyNoInteractions(kafkaPublisherService);
        verify(outboxRepository).findByProcessedFalseOrderByCreatedAtAsc();
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

        when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc())
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
        verify(dlqRepository, never()).save(any());
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


        when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        // simulate Kafka failure
        CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(new RuntimeException("Kafka down"));

        when(kafkaPublisherService.publishEvent(event))
                .thenReturn(future);

        // WHEN
        service.publishPendingEvents();

        // THEN
        assertFalse(event.getProcessed());
        assertEquals(1, event.getRetryCount());

        verify(outboxRepository).save(event);
        verify(dlqRepository, never()).save(any());
    }

    @Test
    void should_move_to_dlq_when_max_retries_reached()  {

        // GIVEN
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(1L);
        event.setPayload("payload");
        event.setEventType(EventConstants.EVENT_INVENTORY_CHECK_REQUESTED);


        // simulate already retried N times
        event.setRetryCount(Constants.MAX_RETRIES_KAFKA - 1);

        when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(new RuntimeException("Kafka down"));

        when(kafkaPublisherService.publishEvent(event))
                .thenReturn(future);

        ArgumentCaptor<OutboxDlqEvent> dlqCaptor =
                ArgumentCaptor.forClass(OutboxDlqEvent.class);

        // WHEN
        service.publishPendingEvents();

        // THEN
        verify(dlqRepository).save(dlqCaptor.capture());

        OutboxDlqEvent dlq = dlqCaptor.getValue();

        assertEquals(event.getId(), dlq.getOriginalEventId());
        assertEquals(event.getAggregateId(), dlq.getAggregateId());
        assertEquals(event.getPayload(), dlq.getPayload());
        assertEquals(org.example.commons.event.utils.Constants.MAX_RETRIES_KAFKA, dlq.getRetryCount());

        assertTrue(event.getProcessed()); // important
        verify(outboxRepository, atLeastOnce()).save(event);
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

        when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(e1, e2));

        // WHEN
        service.publishPendingEvents();

        // THEN
        verify(kafkaPublisherService).publishEvent(e1);
        verify(kafkaPublisherService).publishEvent(e2);

        verify(outboxRepository, atLeast(2)).save(any());
    }
}
