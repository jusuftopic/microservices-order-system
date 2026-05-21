package org.example.orderservice.unit.service;

import org.example.commons.event.EventConstants;
import org.example.orderservice.entity.OutboxEvent;
import org.example.orderservice.service.publisher.KafkaPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaPublisherServiceTest {


    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaPublisherService service;

    @BeforeEach
    void setUp() {
        service = new KafkaPublisherService(kafkaTemplate);
    }

    @Test
    void should_publish_event_to_correct_topic() {

        // GIVEN
        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(1L);
        event.setPayload("payload");
        event.setEventType(EventConstants.EVENT_INVENTORY_CHECK_REQUESTED);

        CompletableFuture<SendResult<String, String>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(
                eq(EventConstants.TOPIC_ORDER_INVENTORY_REQUEST_V1),
                eq("1"),
                eq("payload")
        )).thenReturn(future);

        // WHEN
        CompletableFuture<SendResult<String, String>> result =
                service.publishEvent(event);

        // THEN
        assertNotNull(result);

        verify(kafkaTemplate).send(
                EventConstants.TOPIC_ORDER_INVENTORY_REQUEST_V1,
                "1",
                "payload"
        );
    }

    @Test
    void should_return_future_from_kafka_template() {

        // GIVEN
        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(42L);
        event.setPayload("test");
        event.setEventType(EventConstants.EVENT_INVENTORY_CHECK_REQUESTED);

        CompletableFuture<SendResult<String, String>> future =
                new CompletableFuture<>();

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(future);

        // WHEN
        CompletableFuture<SendResult<String, String>> result =
                service.publishEvent(event);

        // THEN
        assertSame(future, result);
    }

    @Test
    void should_throw_exception_for_unknown_event_type() {

        // GIVEN
        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(1L);
        event.setPayload("payload");
        event.setEventType("UNKNOWN_EVENT");

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class,
                () -> service.publishEvent(event));

        verifyNoInteractions(kafkaTemplate);
    }

}
