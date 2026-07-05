package org.example.orderservice.unit.service.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryReserveRequestedEvent;
import org.example.messagingstarter.outbox.entity.OutboxEvent;
import org.example.messagingstarter.outbox.repository.OutboxRepository;
import org.example.orderservice.service.outbox.OrderOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests {@link OrderOutboxService}
 */
@ExtendWith(MockitoExtension.class)
public class OrderOutboxServiceTest {

    @Mock
    private OutboxRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    private OrderOutboxService service;

    @BeforeEach
    void setUp() {
        service = new OrderOutboxService(
                repository,
                objectMapper
        );
    }

    @Test
    void should_store_outbox_event() throws Exception {

        // GIVEN
        InventoryReserveRequestedEvent payload =
                new InventoryReserveRequestedEvent(
                        1L,
                        List.of(),
                        "corr-123",
                        UUID.randomUUID()
                );

        when(objectMapper.writeValueAsString(payload))
                .thenReturn("{json}");

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        // WHEN
        service.storeEvent(
                1L,
                "ORDER",
                EventConstants.EVENT_INVENTORY_CHECK_REQUESTED,
                payload
        );

        // THEN
        verify(repository).save(captor.capture());

        OutboxEvent event = captor.getValue();

        assertNotNull(event.getId());
        assertEquals("ORDER", event.getAggregateType());
        assertEquals(1L, event.getAggregateId());
        assertEquals(
                EventConstants.EVENT_INVENTORY_CHECK_REQUESTED,
                event.getEventType()
        );
        assertEquals("{json}", event.getPayload());
        assertFalse(event.getProcessed());
        assertNotNull(event.getCreatedAt());
    }


    @Test
    void should_throw_when_serialization_fails() throws Exception {

        // GIVEN
        Object payload = new Object();

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(
                        new JsonProcessingException("boom") {}
                );

        // WHEN + THEN
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.storeEvent(
                        1L,
                        "ORDER",
                        "TEST_EVENT",
                        payload
                )
        );

        assertTrue(
                ex.getMessage().contains(
                        "Failed to serialize outbox payload"
                )
        );

        verify(repository, never()).save(any());
    }

}
