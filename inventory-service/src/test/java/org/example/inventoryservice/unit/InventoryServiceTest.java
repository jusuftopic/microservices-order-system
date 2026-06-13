package org.example.inventoryservice.unit;

import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryReserveRequestedEvent;
import org.example.commons.event.contracts.OrderItemEvent;
import org.example.inventoryservice.entity.InventoryItem;
import org.example.inventoryservice.repository.InboxRepository;
import org.example.inventoryservice.repository.InventoryRepository;
import org.example.inventoryservice.service.InventoryService;
import org.example.inventoryservice.service.outbox.OutboxStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private OutboxStoreService outboxStoreService;

    @Mock
    private InventoryRepository inventoryRepository;


    private InventoryService service;


    @BeforeEach
    void setUp() {
        service = new InventoryService(
                inboxRepository,
                inventoryRepository,
                outboxStoreService
        );
    }


    @Test
    void should_reserve_inventory_and_store_success_outbox_event() throws Exception {

        // GIVEN
        String correlationId = "corr-1";
        UUID messageId = UUID.randomUUID();

        InventoryReserveRequestedEvent event =
                new InventoryReserveRequestedEvent(
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

        // WHEN
        service.processInventory(event);

        // THEN
        verify(inventoryRepository).save(any(InventoryItem.class));
        verify(outboxStoreService).store(
                any(),
                eq(EventConstants.EVENT_INVENTORY_RESERVED),
                eq(1L)
        );
    }


    @Test
    void should_not_process_if_event_already_processed() {

        // GIVEN
        String correlationId = "corr-dup";
        UUID messageId = UUID.randomUUID();

        InventoryReserveRequestedEvent event =
                new InventoryReserveRequestedEvent(1L, List.of(), correlationId, messageId);

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(0);

        // WHEN
        service.processInventory(event);

        // THEN
        verifyNoInteractions(inventoryRepository);
        verifyNoInteractions(outboxStoreService);
    }


    @Test
    void should_store_failure_outbox_event_when_reservation_fails() throws Exception {

        // GIVEN
        String correlationId = "corr-2";
        UUID messageId = UUID.randomUUID();

        InventoryReserveRequestedEvent event =
                new InventoryReserveRequestedEvent(
                        1L,
                        List.of(new OrderItemEvent(99L, 1)),
                        correlationId,
                        messageId
                );

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);
        when(inventoryRepository.findById(99L)).thenReturn(Optional.empty());

        // WHEN
        service.processInventory(event);

        // THEN
        verify(outboxStoreService).store(
                any(),
                eq(EventConstants.EVENT_INVENTORY_FAILED),
                eq(1L)
        );
    }

    @Test
    void should_store_failure_outbox_event_when_out_of_stock() throws Exception {

        // GIVEN
        Long productId = 99L;
        String correlationId = "corr-2";
        UUID messageId = UUID.randomUUID();

        InventoryReserveRequestedEvent event =
                new InventoryReserveRequestedEvent(
                        1L,
                        List.of(new OrderItemEvent(productId, 10)),
                        correlationId,
                        messageId
                );

        final InventoryItem item = InventoryItem.builder()
                .productId(productId)
                .reservedQuantity(50)
                .availableQuantity(5)
                .build();
        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);
        when(inventoryRepository.findById(productId)).thenReturn(Optional.of(item));


        // WHEN
        service.processInventory(event);

        // THEN
        verify(outboxStoreService).store(
                any(),
                eq(EventConstants.EVENT_INVENTORY_FAILED),
                eq(1L)
        );
    }
}
