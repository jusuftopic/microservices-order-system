package org.example.inventoryservice.unit.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.messagingstarter.EventConstants;
import org.example.inventoryservice.service.kafka.KafkaPublisherService;
import org.example.messagingstarter.contracts.*;
import org.example.messagingstarter.outbox.entity.OutboxEvent;
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
    void should_publish_inventory_reserved_event() throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_INVENTORY_RESERVED,
                "{json}"
        );

        InventoryReservedEvent payload =
                new InventoryReservedEvent(1L, "corr-1", UUID.randomUUID());

        when(objectMapper.readValue("{json}", InventoryReservedEvent.class))
                .thenReturn(payload);

        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(
                eq(EventConstants.TOPIC_ORDER_INVENTORY_RESPONSE_V1),
                eq("1"),
                eq(payload)
        )).thenReturn(future);

        // WHEN
        CompletableFuture<SendResult<String, Object>> result =
                service.publishEvent(event);

        // THEN
        assertNotNull(result);
        verify(kafkaTemplate).send(
                eq(EventConstants.TOPIC_ORDER_INVENTORY_RESPONSE_V1),
                eq("1"),
                eq(payload)
        );
    }


    @Test
    void should_publish_inventory_failed_event() throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_INVENTORY_FAILED,
                "{json}"
        );

        InventoryFailedEvent payload =
                new InventoryFailedEvent(1L, "FAIL", "corr", UUID.randomUUID());

        when(objectMapper.readValue("{json}", InventoryFailedEvent.class))
                .thenReturn(payload);

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // WHEN
        service.publishEvent(event);

        // THEN
        verify(kafkaTemplate).send(
                eq(EventConstants.TOPIC_ORDER_INVENTORY_RESPONSE_V1),
                eq("1"),
                eq(payload)
        );
    }

    @Test
    void should_throw_when_deserialization_fails() throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_INVENTORY_RESERVED,
                "{bad-json}"
        );

        when(objectMapper.readValue(anyString(), eq(InventoryReservedEvent.class)))
                .thenThrow(new JsonProcessingException("fail") {});

        // WHEN + THEN
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.publishEvent(event)
        );

        assertTrue(ex.getMessage().contains("Failed to deserialize outbox payload"));

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void should_throw_when_event_type_not_supported() {

        // GIVEN
        OutboxEvent event = createEvent("UNKNOWN_EVENT", "{}");

        // WHEN + THEN
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.publishEvent(event)
        );

        assertTrue(ex.getMessage().contains("Unknown event type"));

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void should_use_correct_topic_for_event() throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_INVENTORY_RESERVED,
                "{json}"
        );

        InventoryReservedEvent payload =
                new InventoryReservedEvent(5L, "corr", UUID.randomUUID());

        when(objectMapper.readValue("{json}", InventoryReservedEvent.class))
                .thenReturn(payload);

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // WHEN
        service.publishEvent(event);

        // THEN
        verify(kafkaTemplate).send(
                eq(EventConstants.TOPIC_ORDER_INVENTORY_RESPONSE_V1),
                eq("1"),
                eq(payload)
        );
    }

    @Test
    void should_publish_inventory_commit_completed_event() throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_INVENTORY_COMMIT_COMPLETED,
                "{json}"
        );

        InventoryCommitCompletedEvent payload =
                new InventoryCommitCompletedEvent(1L, "corr", UUID.randomUUID());

        when(objectMapper.readValue("{json}", InventoryCommitCompletedEvent.class))
                .thenReturn(payload);

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // WHEN
        service.publishEvent(event);

        // THEN
        verify(kafkaTemplate).send(
                eq(EventConstants.TOPIC_ORDER_INVENTORY_FINALIZATION_RESPONSE_V1),
                eq("1"),
                eq(payload)
        );
    }

    @Test
    void should_publish_inventory_commit_failed_event() throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_INVENTORY_COMMIT_FAILED,
                "{json}"
        );

        InventoryCommitFailedEvent payload =
                new InventoryCommitFailedEvent(1L, "FAIL", "corr", UUID.randomUUID());

        when(objectMapper.readValue("{json}", InventoryCommitFailedEvent.class))
                .thenReturn(payload);

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // WHEN
        service.publishEvent(event);

        // THEN
        verify(kafkaTemplate).send(
                eq(EventConstants.TOPIC_ORDER_INVENTORY_FINALIZATION_RESPONSE_V1),
                eq("1"),
                eq(payload)
        );
    }

    @Test
    void should_publish_inventory_release_completed_event() throws Exception {

        // GIVEN
        OutboxEvent event = createEvent(
                EventConstants.EVENT_INVENTORY_RELEASE_COMPLETED,
                "{json}"
        );

        InventoryReleaseCompletedEvent payload =
                new InventoryReleaseCompletedEvent(1L, "corr", UUID.randomUUID());

        when(objectMapper.readValue("{json}", InventoryReleaseCompletedEvent.class))
                .thenReturn(payload);

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // WHEN
        service.publishEvent(event);

        // THEN
        verify(kafkaTemplate).send(
                eq(EventConstants.TOPIC_ORDER_INVENTORY_FINALIZATION_RESPONSE_V1),
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
