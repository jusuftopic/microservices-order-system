package org.example.paymentservice.unit.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.PaymentCompletedEvent;
import org.example.commons.event.contracts.PaymentFailedEvent;
import org.example.paymentservice.entity.OutboxEvent;
import org.example.paymentservice.service.publisher.KafkaPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests {@link KafkaPublisherService}
 */
@ExtendWith(MockitoExtension.class)
public class KafkaPublisherServiceTest {


    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private KafkaPublisherService service;


    @BeforeEach
    void setUp() {
        service = new KafkaPublisherService(
                kafkaTemplate,
                objectMapper
        );
    }


    @Test
    void should_publish_payment_completed_event() throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_PAYMENT_SUCCESS,
                "{json}"
        );

        PaymentCompletedEvent payload =
                new PaymentCompletedEvent(
                        1L,
                        "corr-1",
                        UUID.randomUUID()
                );

        when(objectMapper.readValue(
                "{json}",
                PaymentCompletedEvent.class
        )).thenReturn(payload);

        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(
                eq(EventConstants.TOPIC_ORDER_PAYMENT_RESPONSE_V1),
                eq("1"),
                eq(payload)
        )).thenReturn(future);

        // WHEN
        CompletableFuture<SendResult<String, Object>> result =
                service.publishEvent(event);

        // THEN
        assertNotNull(result);

        verify(kafkaTemplate).send(
                eq(EventConstants.TOPIC_ORDER_PAYMENT_RESPONSE_V1),
                eq("1"),
                eq(payload)
        );
    }


    @Test
    void should_publish_payment_failed_event() throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_PAYMENT_FAILED,
                "{json}"
        );

        PaymentFailedEvent payload =
                new PaymentFailedEvent(
                        1L,
                        "card_declined",
                        "corr-1",
                        UUID.randomUUID()
                );

        when(objectMapper.readValue(
                "{json}",
                PaymentFailedEvent.class
        )).thenReturn(payload);

        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(
                eq(EventConstants.TOPIC_ORDER_PAYMENT_RESPONSE_V1),
                eq("1"),
                eq(payload)
        )).thenReturn(future);

        // WHEN
        CompletableFuture<SendResult<String, Object>> result =
                service.publishEvent(event);

        // THEN
        assertNotNull(result);

        verify(kafkaTemplate).send(
                eq(EventConstants.TOPIC_ORDER_PAYMENT_RESPONSE_V1),
                eq("1"),
                eq(payload)
        );
    }


    @Test
    void should_throw_when_payment_completed_deserialization_fails() throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_PAYMENT_SUCCESS,
                "{bad-json}"
        );

        when(objectMapper.readValue(
                anyString(),
                eq(PaymentCompletedEvent.class)
        )).thenThrow(new JsonProcessingException("error") {});

        // WHEN + THEN
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.publishEvent(event)
        );

        assertTrue(
                ex.getMessage().contains("Failed to deserialize outbox payload")
        );

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void should_throw_when_payment_failed_deserialization_fails() throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_PAYMENT_FAILED,
                "{bad-json}"
        );

        when(objectMapper.readValue(
                anyString(),
                eq(PaymentFailedEvent.class)
        )).thenThrow(new JsonProcessingException("error") {});

        // WHEN + THEN
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.publishEvent(event)
        );

        assertTrue(
                ex.getMessage().contains("Failed to deserialize outbox payload")
        );

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void should_use_correct_topic_for_payment_success_event() throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_PAYMENT_SUCCESS,
                "{json}"
        );

        PaymentCompletedEvent payload =
                new PaymentCompletedEvent(
                        5L,
                        "corr-success",
                        UUID.randomUUID()
                );

        when(objectMapper.readValue(
                "{json}",
                PaymentCompletedEvent.class
        )).thenReturn(payload);

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(
                        CompletableFuture.completedFuture(
                                mock(SendResult.class)
                        )
                );

        // WHEN
        service.publishEvent(event);

        // THEN
        verify(kafkaTemplate).send(
                eq(EventConstants.TOPIC_ORDER_PAYMENT_RESPONSE_V1),
                eq("1"),
                eq(payload)
        );
    }

    @Test
    void should_use_correct_topic_for_payment_failed_event() throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_PAYMENT_FAILED,
                "{json}"
        );

        PaymentFailedEvent payload =
                new PaymentFailedEvent(
                        7L,
                        "provider_down",
                        "corr-failed",
                        UUID.randomUUID()
                );

        when(objectMapper.readValue(
                "{json}",
                PaymentFailedEvent.class
        )).thenReturn(payload);

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(
                        CompletableFuture.completedFuture(
                                mock(SendResult.class)
                        )
                );

        // WHEN
        service.publishEvent(event);

        // THEN
        verify(kafkaTemplate).send(
                eq(EventConstants.TOPIC_ORDER_PAYMENT_RESPONSE_V1),
                eq("1"),
                eq(payload)
        );
    }

    private OutboxEvent createEvent(String type, String payload) {

        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateId(1L);
        event.setEventType(type);
        event.setPayload(payload);

        return event;
    }


}
