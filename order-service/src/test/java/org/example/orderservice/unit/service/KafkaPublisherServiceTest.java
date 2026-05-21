package org.example.orderservice.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryCheckRequestedEvent;
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
    private KafkaTemplate<String, Object> kafkaTemplate;

    private KafkaPublisherService service;

    @BeforeEach
    void setUp() {
        service = new KafkaPublisherService(kafkaTemplate, new ObjectMapper());
    }

    @Test
    void should_publish_event_to_correct_topic() {

        // GIVEN
        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(1L);
        event.setPayload("""
        {
          "orderId": 1,
          "items": [],
          "correlationId": "corr-1"
       }
       """);

        event.setEventType(EventConstants.EVENT_INVENTORY_CHECK_REQUESTED);

        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(
                eq(EventConstants.TOPIC_ORDER_INVENTORY_REQUEST_V1),
                eq("1"),
                any(InventoryCheckRequestedEvent.class)
        )).thenReturn(future);

        // WHEN
        CompletableFuture<SendResult<String, Object>> result =
                service.publishEvent(event);

        // THEN
        assertNotNull(result);

        verify(kafkaTemplate).send(
                eq(EventConstants.TOPIC_ORDER_INVENTORY_REQUEST_V1),
                eq("1"),
                any(InventoryCheckRequestedEvent.class)
        );

    }

    @Test
    void should_return_future_from_kafka_template() {

        // GIVEN
        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(42L);
        event.setPayload("""
        {
          "orderId": 1,
          "items": [],
          "correlationId": "corr-1"
       }
       """);
        event.setEventType(EventConstants.EVENT_INVENTORY_CHECK_REQUESTED);

        CompletableFuture<SendResult<String, Object>> future =
                new CompletableFuture<>();

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(future);

        // WHEN
        CompletableFuture<SendResult<String, Object>> result =
                service.publishEvent(event);

        // THEN
        assertSame(future, result);
    }

    @Test
    void should_throw_exception_for_unknown_event_type() {

        // GIVEN
        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(1L);
        event.setPayload("""
        {
          "orderId": 1,
          "items": [],
          "correlationId": "corr-1"
       }
       """);
        event.setEventType("UNKNOWN_EVENT");

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class,
                () -> service.publishEvent(event));

        verifyNoInteractions(kafkaTemplate);
    }

}
