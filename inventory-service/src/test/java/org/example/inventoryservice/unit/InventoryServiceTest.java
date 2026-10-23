package org.example.inventoryservice.unit;

import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.commands.ReserveInventoryCommand;
import org.example.messagingstarter.contracts.commands.CommitInventoryCommand;
import org.example.messagingstarter.contracts.commands.ReleaseInventoryCommand;
import org.example.messagingstarter.contracts.OrderItemEvent;
import org.example.inventoryservice.entity.InventoryItem;
import org.example.inventoryservice.metrics.InventoryMetrics;
import org.example.inventoryservice.repository.InventoryRepository;
import org.example.inventoryservice.service.InventoryService;
import org.example.inventoryservice.service.outbox.OutboxStoreService;
import org.example.messagingstarter.inbox.repository.InboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private InventoryMetrics inventoryMetrics;


    private InventoryService service;


    @BeforeEach
    void setUp() {
        service = new InventoryService(
                inboxRepository,
                inventoryRepository,
                outboxStoreService,
                inventoryMetrics
        );
    }


    @Test
    void should_reserve_all_items_and_store_one_success_outbox_event() {

        // GIVEN
        String correlationId = "corr-1";
        UUID messageId = UUID.randomUUID();
        ReserveInventoryCommand event =
                new ReserveInventoryCommand(
                        1L,
                        List.of(new OrderItemEvent(10L, 2), new OrderItemEvent(20L, 3)),
                        correlationId,
                        messageId
                );

        InventoryItem firstItem = InventoryItem.builder()
                .productId(10L)
                .availableQuantity(10)
                .reservedQuantity(0)
                .build();
        InventoryItem secondItem = InventoryItem.builder()
                .productId(20L)
                .availableQuantity(8)
                .reservedQuantity(1)
                .build();

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);
        when(inventoryRepository.findAllById(Set.of(10L, 20L)))
                .thenReturn(List.of(firstItem, secondItem));

        // WHEN
        service.processInventory(event);

        // THEN
        assertThat(firstItem.getAvailableQuantity()).isEqualTo(8);
        assertThat(firstItem.getReservedQuantity()).isEqualTo(2);
        assertThat(secondItem.getAvailableQuantity()).isEqualTo(5);
        assertThat(secondItem.getReservedQuantity()).isEqualTo(4);
        verify(inventoryRepository).saveAll(any());
        verify(outboxStoreService).store(
                any(),
                eq(EventConstants.EVENT_INVENTORY_RESERVED),
                eq(1L)
        );
        verifyNoMoreInteractions(outboxStoreService);
    }


    @Test
    void should_not_process_if_event_already_processed() {

        // GIVEN
        String correlationId = "corr-dup";
        UUID messageId = UUID.randomUUID();

        ReserveInventoryCommand event =
                new ReserveInventoryCommand(1L, List.of(), correlationId, messageId);

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(0);

        // WHEN
        service.processInventory(event);

        // THEN
        verifyNoInteractions(inventoryRepository);
        verifyNoInteractions(outboxStoreService);
    }


    @Test
    void should_not_reserve_any_item_when_a_later_product_is_missing() {

        // GIVEN
        String correlationId = "corr-2";
        UUID messageId = UUID.randomUUID();
        ReserveInventoryCommand event =
                new ReserveInventoryCommand(
                        1L,
                        List.of(new OrderItemEvent(10L, 2), new OrderItemEvent(99L, 1)),
                        correlationId,
                        messageId
                );
        InventoryItem availableItem = InventoryItem.builder()
                .productId(10L)
                .availableQuantity(10)
                .reservedQuantity(0)
                .build();

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);
        when(inventoryRepository.findAllById(Set.of(10L, 99L)))
                .thenReturn(List.of(availableItem));

        // WHEN
        service.processInventory(event);

        // THEN
        assertThat(availableItem.getAvailableQuantity()).isEqualTo(10);
        assertThat(availableItem.getReservedQuantity()).isZero();
        verify(inventoryRepository, never()).save(any());
        verify(inventoryRepository, never()).saveAll(any());
        verify(outboxStoreService).store(
                any(),
                eq(EventConstants.EVENT_INVENTORY_FAILED),
                eq(1L)
        );
        verifyNoMoreInteractions(outboxStoreService);
    }

    @Test
    void should_not_reserve_any_item_when_a_later_product_is_out_of_stock() {

        // GIVEN
        Long productId1 = 99L;
        Long productId2 = 98L;
        String correlationId = "corr-2";
        UUID messageId = UUID.randomUUID();

        ReserveInventoryCommand event =
                new ReserveInventoryCommand(
                        1L,
                        List.of(
                                new OrderItemEvent(productId1, 10),
                                new OrderItemEvent(productId2, 10)
                        ),
                        correlationId,
                        messageId
                );

        final InventoryItem item1 = InventoryItem.builder()
                .productId(productId1)
                .reservedQuantity(50)
                .availableQuantity(15)
                .build();
        final InventoryItem item2 = InventoryItem.builder()
                .productId(productId2)
                .reservedQuantity(50)
                .availableQuantity(5)
                .build();

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);
        when(inventoryRepository.findAllById(Set.of(productId1, productId2)))
                .thenReturn(List.of(item1, item2));


        // WHEN
        service.processInventory(event);

        // THEN
        assertThat(item1.getAvailableQuantity()).isEqualTo(15);
        assertThat(item1.getReservedQuantity()).isEqualTo(50);
        assertThat(item2.getAvailableQuantity()).isEqualTo(5);
        assertThat(item2.getReservedQuantity()).isEqualTo(50);
        verify(inventoryRepository, never()).save(any());
        verify(inventoryRepository, never()).saveAll(any());
        verify(outboxStoreService).store(
                any(),
                eq(EventConstants.EVENT_INVENTORY_FAILED),
                eq(1L)
        );
        verifyNoMoreInteractions(outboxStoreService);
    }

    @Test
    void should_check_combined_quantity_when_the_same_product_appears_twice() {
        UUID messageId = UUID.randomUUID();
        ReserveInventoryCommand event = new ReserveInventoryCommand(
                1L,
                List.of(new OrderItemEvent(10L, 2), new OrderItemEvent(10L, 3)),
                "corr-duplicate-product",
                messageId
        );
        InventoryItem item = InventoryItem.builder()
                .productId(10L)
                .availableQuantity(4)
                .reservedQuantity(0)
                .build();

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);
        when(inventoryRepository.findAllById(Set.of(10L))).thenReturn(List.of(item));

        service.processInventory(event);

        assertThat(item.getAvailableQuantity()).isEqualTo(4);
        assertThat(item.getReservedQuantity()).isZero();
        verify(inventoryRepository, never()).save(any());
        verify(inventoryRepository, never()).saveAll(any());
        verify(outboxStoreService).store(
                any(), eq(EventConstants.EVENT_INVENTORY_FAILED), eq(1L));
        verifyNoMoreInteractions(outboxStoreService);
    }

    @Test
    void should_commit_all_items_and_store_one_success_event() {
        UUID messageId = UUID.randomUUID();
        CommitInventoryCommand event = new CommitInventoryCommand(1L,
                List.of(new OrderItemEvent(10L, 2), new OrderItemEvent(20L, 3)),
                "corr-commit", messageId);
        InventoryItem first = inventoryItem(10L, 8, 5);
        InventoryItem second = inventoryItem(20L, 4, 4);
        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);
        when(inventoryRepository.findAllById(Set.of(10L, 20L))).thenReturn(List.of(first, second));

        service.processCommit(event);

        assertThat(first.getAvailableQuantity()).isEqualTo(8);
        assertThat(first.getReservedQuantity()).isEqualTo(3);
        assertThat(second.getAvailableQuantity()).isEqualTo(4);
        assertThat(second.getReservedQuantity()).isEqualTo(1);
        verify(inventoryRepository).saveAll(any());
        verify(outboxStoreService).store(any(), eq(EventConstants.EVENT_INVENTORY_COMMIT_COMPLETED), eq(1L));
        verifyNoMoreInteractions(outboxStoreService);
    }

    @Test
    void should_not_commit_any_item_when_a_later_product_is_missing() {
        UUID messageId = UUID.randomUUID();
        CommitInventoryCommand event = new CommitInventoryCommand(1L,
                List.of(new OrderItemEvent(10L, 2), new OrderItemEvent(99L, 1)),
                "corr-commit-missing", messageId);
        InventoryItem first = inventoryItem(10L, 8, 5);
        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);
        when(inventoryRepository.findAllById(Set.of(10L, 99L))).thenReturn(List.of(first));

        service.processCommit(event);

        assertThat(first.getAvailableQuantity()).isEqualTo(8);
        assertThat(first.getReservedQuantity()).isEqualTo(5);
        verify(inventoryRepository, never()).saveAll(any());
        verify(outboxStoreService).store(any(), eq(EventConstants.EVENT_INVENTORY_COMMIT_FAILED), eq(1L));
        verifyNoMoreInteractions(outboxStoreService);
    }

    @Test
    void should_not_commit_any_item_when_a_later_product_has_insufficient_reservation() {
        UUID messageId = UUID.randomUUID();
        CommitInventoryCommand event = new CommitInventoryCommand(1L,
                List.of(new OrderItemEvent(10L, 2), new OrderItemEvent(20L, 3)),
                "corr-commit-insufficient", messageId);
        InventoryItem first = inventoryItem(10L, 8, 5);
        InventoryItem second = inventoryItem(20L, 4, 1);
        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);
        when(inventoryRepository.findAllById(Set.of(10L, 20L))).thenReturn(List.of(first, second));

        service.processCommit(event);

        assertThat(first.getReservedQuantity()).isEqualTo(5);
        assertThat(second.getReservedQuantity()).isEqualTo(1);
        verify(inventoryRepository, never()).saveAll(any());
        verify(outboxStoreService).store(any(), eq(EventConstants.EVENT_INVENTORY_COMMIT_FAILED), eq(1L));
        verifyNoMoreInteractions(outboxStoreService);
    }

    @Test
    void should_release_all_items_and_store_one_completion_event() {
        UUID messageId = UUID.randomUUID();
        ReleaseInventoryCommand event = new ReleaseInventoryCommand(1L,
                List.of(new OrderItemEvent(10L, 2), new OrderItemEvent(20L, 3)),
                "corr-release", messageId);
        InventoryItem first = inventoryItem(10L, 8, 5);
        InventoryItem second = inventoryItem(20L, 4, 4);
        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);
        when(inventoryRepository.findAllById(Set.of(10L, 20L))).thenReturn(List.of(first, second));

        service.processRelease(event);

        assertThat(first.getAvailableQuantity()).isEqualTo(10);
        assertThat(first.getReservedQuantity()).isEqualTo(3);
        assertThat(second.getAvailableQuantity()).isEqualTo(7);
        assertThat(second.getReservedQuantity()).isEqualTo(1);
        verify(inventoryRepository).saveAll(any());
        verify(outboxStoreService).store(any(), eq(EventConstants.EVENT_INVENTORY_RELEASE_COMPLETED), eq(1L));
        verifyNoMoreInteractions(outboxStoreService);
    }

    @Test
    void should_not_release_any_item_or_complete_when_a_later_product_is_missing() {
        UUID messageId = UUID.randomUUID();
        ReleaseInventoryCommand event = new ReleaseInventoryCommand(1L,
                List.of(new OrderItemEvent(10L, 2), new OrderItemEvent(99L, 1)),
                "corr-release-missing", messageId);
        InventoryItem first = inventoryItem(10L, 8, 5);
        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);
        when(inventoryRepository.findAllById(Set.of(10L, 99L))).thenReturn(List.of(first));

        assertThatThrownBy(() -> service.processRelease(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("product 99 not found");

        assertThat(first.getAvailableQuantity()).isEqualTo(8);
        assertThat(first.getReservedQuantity()).isEqualTo(5);
        verify(inventoryRepository, never()).saveAll(any());
        verifyNoInteractions(outboxStoreService);
    }

    @Test
    void should_not_release_any_item_or_complete_when_a_later_reservation_is_insufficient() {
        UUID messageId = UUID.randomUUID();
        ReleaseInventoryCommand event = new ReleaseInventoryCommand(1L,
                List.of(new OrderItemEvent(10L, 2), new OrderItemEvent(20L, 3)),
                "corr-release-insufficient", messageId);
        InventoryItem first = inventoryItem(10L, 8, 5);
        InventoryItem second = inventoryItem(20L, 4, 1);
        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);
        when(inventoryRepository.findAllById(Set.of(10L, 20L))).thenReturn(List.of(first, second));

        assertThatThrownBy(() -> service.processRelease(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("product 20 requested 3 but only 1 reserved");

        assertThat(first.getAvailableQuantity()).isEqualTo(8);
        assertThat(first.getReservedQuantity()).isEqualTo(5);
        assertThat(second.getAvailableQuantity()).isEqualTo(4);
        assertThat(second.getReservedQuantity()).isEqualTo(1);
        verify(inventoryRepository, never()).saveAll(any());
        verifyNoInteractions(outboxStoreService);
    }

    private InventoryItem inventoryItem(Long productId, int available, int reserved) {
        return InventoryItem.builder()
                .productId(productId)
                .availableQuantity(available)
                .reservedQuantity(reserved)
                .build();
    }
}
