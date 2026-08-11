package org.example.orderservice.unit.service.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.commands.ReserveInventoryCommand;
import org.example.messagingstarter.contracts.events.OrderLifecycleTransitionedEvent;
import org.example.messagingstarter.outbox.entity.OutboxEvent;
import org.example.orderservice.service.kafka.KafkaPublisherService;
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
        service = new KafkaPublisherService(kafkaTemplate, new ObjectMapper().findAndRegisterModules());
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
                any(ReserveInventoryCommand.class)
        )).thenReturn(future);

        // WHEN
        CompletableFuture<SendResult<String, Object>> result =
                service.publishEvent(event);

        // THEN
        assertNotNull(result);

        verify(kafkaTemplate).send(
                eq(EventConstants.TOPIC_ORDER_INVENTORY_REQUEST_V1),
                eq("1"),
                any(ReserveInventoryCommand.class)
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
    void should_publish_lifecycle_event_to_investigation_topic() {

        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(42L);
        event.setEventType(EventConstants.EVENT_ORDER_LIFECYCLE_TRANSITIONED);
        event.setPayload("""
                {
                  "orderId": 42,
                  "previousStatus": "PAYMENT_FAILED",
                  "newStatus": "FAILED",
                  "reasonCode": "COMPENSATION_COMPLETED",
                  "sourceService": "INVENTORY_SERVICE",
                  "sourceEventType": "EVENT_INVENTORY_RELEASE_COMPLETED",
                  "causationId": "3e4b3ce2-10d1-4c3e-b967-58fbc1773f69",
                  "orchestrationDecision": null,
                  "compensation": {"required": false, "type": null},
                  "occurredAt": "2026-08-11T10:15:00Z",
                  "eventVersion": 1,
                  "correlationId": "corr-42",
                  "messageId": "2b997737-7918-442b-bbd8-c26e52fce083"
                }
                """);

        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(
                eq(EventConstants.TOPIC_ORDER_LIFECYCLE_V1),
                eq("42"),
                any(OrderLifecycleTransitionedEvent.class)
        )).thenReturn(future);

        assertSame(future, service.publishEvent(event));

        verify(kafkaTemplate).send(
                eq(EventConstants.TOPIC_ORDER_LIFECYCLE_V1),
                eq("42"),
                any(OrderLifecycleTransitionedEvent.class)
        );
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
