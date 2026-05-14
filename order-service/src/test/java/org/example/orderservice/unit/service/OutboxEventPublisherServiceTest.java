package org.example.orderservice.unit.service;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.example.orderservice.entity.OutboxDlqEvent;
import org.example.orderservice.entity.OutboxEvent;
import org.example.orderservice.repository.OutboxDlqRepository;
import org.example.orderservice.repository.OutboxRepository;
import org.example.orderservice.service.publisher.OutboxEventPublisherService;
import org.example.orderservice.utils.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
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
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxEventPublisherService service;

    @BeforeEach
    public void setUp() {
        service = new OutboxEventPublisherService(
                outboxRepository, dlqRepository, kafkaTemplate
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
        verifyNoInteractions(kafkaTemplate);
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

        when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        // simulate Kafka success
        RecordMetadata metadata = mock(RecordMetadata.class);

        SendResult<String, String> sendResult = mock(SendResult.class);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);

        CompletableFuture<SendResult<String, String>> future =
                CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
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

        when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        // simulate Kafka failure
        CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(new RuntimeException("Kafka down"));

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
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

        // simulate already retried N times
        event.setRetryCount(Constants.MAX_RETRIES - 1);

        when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        CompletableFuture future = new CompletableFuture();
        future.completeExceptionally(new RuntimeException("Kafka down"));

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
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
        assertEquals(Constants.MAX_RETRIES, dlq.getRetryCount());

        assertTrue(event.getProcessed()); // important
        verify(outboxRepository, atLeastOnce()).save(event);
    }

    @Test
    void should_process_multiple_events() throws Exception {

        // GIVEN
        OutboxEvent e1 = new OutboxEvent();
        e1.setId(UUID.randomUUID());
        e1.setAggregateId(1L);
        e1.setPayload("p1");

        OutboxEvent e2 = new OutboxEvent();
        e2.setId(UUID.randomUUID());
        e2.setAggregateId(2L);
        e2.setPayload("p2");

        when(outboxRepository.findByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(e1, e2));


        SendResult<String, String> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, String>> future =
                CompletableFuture.completedFuture(sendResult);

        // WHEN
        service.publishPendingEvents();

        // THEN
        verify(kafkaTemplate, times(2))
                .send(anyString(), anyString(), anyString());

        verify(outboxRepository, atLeast(2)).save(any());
    }
}
