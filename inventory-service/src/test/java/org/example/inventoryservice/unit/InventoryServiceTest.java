package org.example.inventoryservice.unit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryCheckRequestedEvent;
import org.example.commons.event.contracts.OrderItemEvent;
import org.example.inventoryservice.entity.InventoryItem;
import org.example.inventoryservice.entity.OutboxEvent;
import org.example.inventoryservice.repository.InboxRepository;
import org.example.inventoryservice.repository.InventoryRepository;
import org.example.inventoryservice.repository.OutboxRepository;
import org.example.inventoryservice.service.InventoryService;
import org.example.inventoryservice.service.OutboxDlqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests {@link org.example.inventoryservice.service.InventoryService}
 */
@ExtendWith(MockitoExtension.class)
public class InventoryServiceTest {

    @Mock
    private InboxRepository inboxRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private OutboxDlqService outboxDlqService;

    @Mock
    private ObjectMapper objectMapper;


    private InventoryService service;


    @BeforeEach
    void setUp() {
        service = new InventoryService(
                inboxRepository,
                outboxRepository,
                inventoryRepository,
                outboxDlqService,
                objectMapper
        );
    }


    @Test
    void should_reserve_inventory_and_store_success_outbox_event() throws Exception {

        // GIVEN
        String correlationId = "corr-1";
        UUID messageId = UUID.randomUUID();

        InventoryCheckRequestedEvent event =
                new InventoryCheckRequestedEvent(
                        1L,
                        List.of(new OrderItemEvent(10L, 2)),
                        correlationId,
                        messageId
                );

        InventoryItem item = InventoryItem.builder()
                .productId(10L)
                .availableQuantity(10)
                .reservedQuantity(0)
                .build();

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);
        when(inventoryRepository.findById(10L)).thenReturn(Optional.of(item));
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

        // WHEN
        service.processInventory(event);

        // THEN
        verify(inventoryRepository).save(any(InventoryItem.class));
        verify(outboxRepository).save(captor.capture());

        OutboxEvent outbox = captor.getValue();

        assertEquals(EventConstants.EVENT_INVENTORY_RESERVED, outbox.getEventType());
        assertEquals("{json}", outbox.getPayload());
        assertEquals(1L, outbox.getAggregateId());
        assertFalse(outbox.getProcessed());
    }


    @Test
    void should_not_process_if_event_already_processed() {

        // GIVEN
        String correlationId = "corr-dup";
        UUID messageId = UUID.randomUUID();

        InventoryCheckRequestedEvent event =
                new InventoryCheckRequestedEvent(1L, List.of(), correlationId, messageId);

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(0);

        // WHEN
        service.processInventory(event);

        // THEN
        verifyNoInteractions(inventoryRepository);
        verifyNoInteractions(outboxRepository);
    }


    @Test
    void should_store_failure_outbox_event_when_reservation_fails() throws Exception {

        // GIVEN
        String correlationId = "corr-2";
        UUID messageId = UUID.randomUUID();

        InventoryCheckRequestedEvent event =
                new InventoryCheckRequestedEvent(
                        1L,
                        List.of(new OrderItemEvent(99L, 1)),
                        correlationId,
                        messageId
                );

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);
        when(inventoryRepository.findById(99L)).thenReturn(Optional.empty());

        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);

        // WHEN
        service.processInventory(event);

        // THEN
        verify(outboxRepository).save(captor.capture());

        OutboxEvent outbox = captor.getValue();

        assertEquals(EventConstants.EVENT_INVENTORY_FAILED, outbox.getEventType());
        assertEquals("{json}", outbox.getPayload());
    }


    @Test
    void should_store_event_in_dlq_when_serialization_fails() throws Exception {

        // GIVEN
        String correlationId = "corr-3";
        UUID messageId = UUID.randomUUID();

        InventoryCheckRequestedEvent event =
                new InventoryCheckRequestedEvent(
                        1L,
                        List.of(new OrderItemEvent(10L, 1)),
                        correlationId,
                        messageId
                );

        InventoryItem item = InventoryItem.builder()
                .productId(10L)
                .availableQuantity(10)
                .reservedQuantity(0)
                .build();

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);
        when(inventoryRepository.findById(10L)).thenReturn(Optional.of(item));

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("fail") {});

        // WHEN
        service.processInventory(event);

        // THEN
        verify(outboxRepository, never()).save(any());
        verify(outboxDlqService).storeOutboxDlq(
                isNull(),
                eq(1L),
                eq(EventConstants.EVENT_INVENTORY_RESERVED),
                any(),
                eq(0),
                any()
        );
    }
}
