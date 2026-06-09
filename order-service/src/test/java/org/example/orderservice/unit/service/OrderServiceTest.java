package org.example.orderservice.unit.service;

import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.InventoryCheckRequestedEvent;
import org.example.commons.event.contracts.InventoryFailedEvent;
import org.example.commons.event.contracts.InventoryReservedEvent;
import org.example.commons.event.contracts.PaymentRequestedEvent;
import org.example.orderservice.dto.request.OrderItemRequest;
import org.example.orderservice.dto.request.OrderRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.entity.OrderItem;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.repository.InboxRepository;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.service.OrderService;
import org.example.orderservice.service.outbox.OrderOutboxService;
import org.example.orderservice.service.workflow.OrderWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests {@link OrderService}
 */
@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private OrderOutboxService outboxService;

    @Mock
    private OrderWorkflowService workflowService;

    @Mock
    private InboxRepository inboxRepository;

    /* class under test */
    private OrderService orderService;

    @BeforeEach
    public void setUp() {
        orderService = new OrderService(
                repository, inboxRepository, outboxService, workflowService
        );
    }

    @Test
    void should_create_order_and_store_outbox_event() throws Exception {

        // GIVEN
        OrderRequest request = new OrderRequest(
                "test@mail.com",
                List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(2L, 1)
                ),
                "desc"
        );

        Order savedOrder = Order.builder()
                .id(1L)
                .customerEmail("test@mail.com")
                .description("desc")
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        request.items().forEach(item -> savedOrder.addItem(OrderItem.builder()
                        .productId(item.productId())
                        .quantity(item.quantity())
                .build()));

        when(repository.save(any(Order.class))).thenReturn(savedOrder);


        // WHEN
        OrderResponse response = orderService.createOrder(request);

        // THEN
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("CREATED", response.status());

        verify(repository).save(any(Order.class));

        verify(outboxService).storeEvent(
                eq(1L),
                eq("ORDER"),
                eq(EventConstants.EVENT_INVENTORY_CHECK_REQUESTED),
                any(InventoryCheckRequestedEvent.class));

    }


    @Test
    void should_return_order_when_found() {

        // GIVEN
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.CREATED)
                .customerEmail("test@mail.com")
                .items(List.of(
                        OrderItem.builder()
                                .productId(1L)
                                .quantity(2)
                                .build()
                        )
                )
                .description("desc")
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(order));

        // WHEN
        OrderResponse response = orderService.getOrder(1L);

        // THEN
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("CREATED", response.status());
        assertEquals(1, response.items().size());
    }

    @Test
    void should_return_null_when_order_not_found() {

        // GIVEN
        when(repository.findById(1L)).thenReturn(Optional.empty());

        // WHEN
        OrderResponse response = orderService.getOrder(1L);

        // THEN
        assertNull(response);
    }


    @Test
    void should_handle_inventory_reserved_and_create_payment_outbox_event() throws Exception {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .customerEmail("test@mail.com")
                .status(OrderStatus.CREATED)
                .build();

        InventoryReservedEvent event = new InventoryReservedEvent(
                orderId,
                "corr-123",
                UUID.randomUUID()
        );

        when(inboxRepository.insertIfNotExists(event.messageId())).thenReturn(1);
        when(workflowService.markInventoryProcessing(orderId)).thenReturn(order);

        // WHEN
        orderService.handleInventoryReserved(event);

        // THEN
        verify(inboxRepository).insertIfNotExists(event.messageId());
        verify(workflowService).markInventoryProcessing(orderId);

        verify(outboxService).storeEvent(
                eq(orderId),
                eq("ORDER"),
                eq(EventConstants.EVENT_PAYMENT_REQUESTED),
                any(PaymentRequestedEvent.class)
        );
    }


    @Test
    void should_handle_inventory_failed_and_mark_order_failed() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.CREATED)
                .build();

        InventoryFailedEvent event = new InventoryFailedEvent(
                orderId,
                "OUT_OF_STOCK",
                "corr-123",
                UUID.randomUUID()
        );

        when(workflowService.markFailed(orderId)).thenReturn(order);

        // WHEN
        orderService.handleInventoryFailed(event);

        // THEN
        verify(workflowService).markFailed(orderId);
        verifyNoInteractions(outboxService);
    }

}
