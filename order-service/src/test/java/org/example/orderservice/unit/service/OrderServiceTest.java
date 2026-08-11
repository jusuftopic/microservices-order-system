package org.example.orderservice.unit.service;

import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.commands.*;
import org.example.messagingstarter.contracts.events.*;
import org.example.orderservice.dto.request.OrderItemRequest;
import org.example.orderservice.dto.request.OrderRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.entity.OrderItem;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.metrics.OrderMetrics;
import org.example.messagingstarter.inbox.repository.InboxRepository;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.service.OrderCompensationService;
import org.example.orderservice.service.OrderService;
import org.example.orderservice.service.outbox.OrderOutboxService;
import org.example.orderservice.service.workflow.OrderWorkflowService;
import org.example.orderservice.service.workflow.OrderTransitionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
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

    @Mock
    private OrderMetrics orderMetrics;

    @Mock
    private OrderCompensationService compensationService;

    /* class under test */
    private OrderService orderService;

    @BeforeEach
    public void setUp() {
        orderService = new OrderService(
                repository, inboxRepository, outboxService, workflowService,
                compensationService, orderMetrics
        );
    }

    @Test
    void should_create_order_and_store_outbox_event() {

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
                any(ReserveInventoryCommand.class));
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
        when(workflowService.updateStatus(
                eq(orderId),
                eq(OrderStatus.INVENTORY_RESERVE_COMPLETED),
                any(OrderTransitionContext.class)
        )).thenReturn(order);

        // WHEN
        orderService.handleInventoryReserved(event);

        // THEN
        verify(inboxRepository).insertIfNotExists(event.messageId());
        verify(workflowService).updateStatus(
                eq(orderId),
                eq(OrderStatus.INVENTORY_RESERVE_COMPLETED),
                any(OrderTransitionContext.class)
        );

        verify(outboxService).storeEvent(
                eq(orderId),
                eq("ORDER"),
                eq(EventConstants.EVENT_PAYMENT_REQUESTED),
                any(ProcessPaymentCommand.class)
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

        when(workflowService.updateStatus(
                eq(orderId),
                eq(OrderStatus.INVENTORY_RESERVE_FAILED),
                any(OrderTransitionContext.class)
        )).thenReturn(order);

        // WHEN
        orderService.handleInventoryFailed(event);

        // THEN
        verify(workflowService).updateStatus(
                eq(orderId),
                eq(OrderStatus.INVENTORY_RESERVE_FAILED),
                any(OrderTransitionContext.class)
        );
        verifyNoInteractions(outboxService);
    }


    @Test
    void should_handle_payment_completed_and_create_inventory_commit_outbox_event() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .customerEmail("test@mail.com")
                .status(OrderStatus.INVENTORY_RESERVE_COMPLETED)
                .items(List.of(
                        OrderItem.builder()
                                .productId(1L)
                                .quantity(2)
                                .build(),
                        OrderItem.builder()
                                .productId(2L)
                                .quantity(1)
                                .build()
                ))
                .build();

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                orderId,
                "corr-123",
                UUID.randomUUID()
        );

        when(inboxRepository.insertIfNotExists(event.messageId()))
                .thenReturn(1);

        when(workflowService.updateStatus(
                eq(orderId),
                eq(OrderStatus.PAYMENT_COMPLETED),
                any(OrderTransitionContext.class)
        )).thenReturn(order);

        // WHEN
        orderService.handlePaymentCompleted(event);

        // THEN
        verify(inboxRepository)
                .insertIfNotExists(event.messageId());

        verify(workflowService)
                .updateStatus(
                        eq(orderId),
                        eq(OrderStatus.PAYMENT_COMPLETED),
                        any(OrderTransitionContext.class)
                );

        verify(outboxService).storeEvent(
                eq(orderId),
                eq("ORDER"),
                eq(EventConstants.EVENT_INVENTORY_COMMIT_REQUESTED),
                any(CommitInventoryCommand.class)
        );
    }


    @Test
    void should_handle_payment_failed_and_create_inventory_release_outbox_event() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .customerEmail("test@mail.com")
                .status(OrderStatus.INVENTORY_RESERVE_COMPLETED)
                .items(List.of(
                        OrderItem.builder()
                                .productId(1L)
                                .quantity(2)
                                .build(),
                        OrderItem.builder()
                                .productId(2L)
                                .quantity(1)
                                .build()
                ))
                .build();

        PaymentFailedEvent event = new PaymentFailedEvent(
                orderId,
                "DECLINED",
                "corr-123",
                UUID.randomUUID()
        );

        when(inboxRepository.insertIfNotExists(event.messageId()))
                .thenReturn(1);

        when(workflowService.updateStatus(
                eq(orderId),
                eq(OrderStatus.PAYMENT_FAILED),
                any(OrderTransitionContext.class)
        )).thenReturn(order);

        // WHEN
        orderService.handlePaymentFailed(event);

        // THEN
        verify(inboxRepository)
                .insertIfNotExists(event.messageId());

        ArgumentCaptor<OrderTransitionContext> transitionContext =
                ArgumentCaptor.forClass(OrderTransitionContext.class);
        verify(workflowService).updateStatus(
                eq(orderId),
                eq(OrderStatus.PAYMENT_FAILED),
                transitionContext.capture()
        );

        ArgumentCaptor<ReleaseInventoryCommand> releaseCommand =
                ArgumentCaptor.forClass(ReleaseInventoryCommand.class);
        verify(outboxService).storeEvent(
                eq(orderId),
                eq("ORDER"),
                eq(EventConstants.EVENT_INVENTORY_RELEASE_REQUESTED),
                releaseCommand.capture()
        );

        assertTrue(transitionContext.getValue().compensation().required());
        assertEquals("INVENTORY_RELEASE", transitionContext.getValue().compensation().type());
        assertEquals(
                releaseCommand.getValue().messageId(),
                transitionContext.getValue().orchestrationDecision().commandId()
        );
    }


    @Test
    void should_handle_inventory_commit_completed_and_send_notification() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .customerEmail("test@mail.com")
                .status(OrderStatus.INVENTORY_COMMIT_COMPLETED) // current before transition
                .createdAt(LocalDateTime.now())
                .build();

        InventoryCommitCompletedEvent event =
                new InventoryCommitCompletedEvent(
                        orderId,
                        "corr-123",
                        UUID.randomUUID()
                );

        when(inboxRepository.insertIfNotExists(event.messageId()))
                .thenReturn(1);

        when(workflowService.updateStatus(
                eq(orderId),
                eq(OrderStatus.INVENTORY_COMMIT_COMPLETED),
                any(OrderTransitionContext.class)
        )).thenReturn(order);

        when(workflowService.updateStatus(
                eq(orderId),
                eq(OrderStatus.COMPLETED),
                any(OrderTransitionContext.class)
        )).thenReturn(order);

        // WHEN
        orderService.handleInventoryCommitCompleted(event);

        // THEN
        verify(inboxRepository)
                .insertIfNotExists(event.messageId());

        verify(workflowService)
                .updateStatus(
                        eq(orderId),
                        eq(OrderStatus.INVENTORY_COMMIT_COMPLETED),
                        any(OrderTransitionContext.class)
                );
        verify(workflowService)
                .updateStatus(
                        eq(orderId),
                        eq(OrderStatus.COMPLETED),
                        any(OrderTransitionContext.class)
                );

        verify(outboxService).storeEvent(
                eq(orderId),
                eq("ORDER"),
                eq(EventConstants.EVENT_NOTIFICATION_REQUESTED),
                any(SendNotificationCommand.class)
        );
    }


    @Test
    void should_not_process_inventory_commit_completed_event_twice() {

        // GIVEN
        InventoryCommitCompletedEvent event =
                new InventoryCommitCompletedEvent(
                        1L,
                        "corr-123",
                        UUID.randomUUID()
                );

        when(inboxRepository.insertIfNotExists(event.messageId()))
                .thenReturn(0);

        // WHEN
        orderService.handleInventoryCommitCompleted(event);

        // THEN
        verify(inboxRepository)
                .insertIfNotExists(event.messageId());

        verifyNoInteractions(workflowService);
        verifyNoInteractions(outboxService);
    }


    @Test
    void should_handle_inventory_release_completed_and_send_notification() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .customerEmail("test@mail.com")
                .status(OrderStatus.PAYMENT_FAILED)
                .build();

        InventoryReleaseCompletedEvent event =
                new InventoryReleaseCompletedEvent(
                        orderId,
                        "corr-123",
                        UUID.randomUUID()
                );

        when(inboxRepository.insertIfNotExists(event.messageId()))
                .thenReturn(1);

        when(workflowService.updateStatus(
                eq(orderId),
                eq(OrderStatus.FAILED),
                any(OrderTransitionContext.class)
        )).thenReturn(order);

        // WHEN
        orderService.handleInventoryReleaseCompleted(event);

        // THEN
        verify(inboxRepository)
                .insertIfNotExists(event.messageId());

        verify(workflowService)
                .updateStatus(
                        eq(orderId),
                        eq(OrderStatus.FAILED),
                        any(OrderTransitionContext.class)
                );

        verify(outboxService).storeEvent(
                eq(orderId),
                eq("ORDER"),
                eq(EventConstants.EVENT_NOTIFICATION_REQUESTED),
                any(SendNotificationCommand.class)
        );
    }

    @Test
    void should_not_process_inventory_release_completed_twice() {

        // GIVEN
        InventoryReleaseCompletedEvent event =
                new InventoryReleaseCompletedEvent(
                        1L,
                        "corr",
                        UUID.randomUUID()
                );

        when(inboxRepository.insertIfNotExists(event.messageId()))
                .thenReturn(0);

        // WHEN
        orderService.handleInventoryReleaseCompleted(event);

        // THEN
        verify(inboxRepository)
                .insertIfNotExists(event.messageId());

        verifyNoInteractions(workflowService);
        verifyNoInteractions(outboxService);
    }

    @Test
    void should_handle_inventory_commit_failed_and_trigger_refund_and_notification() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .customerEmail("test@mail.com")
                .status(OrderStatus.PAYMENT_COMPLETED)
                .items(List.of(
                        OrderItem.builder().productId(1L).quantity(2).build()
                ))
                .build();

        InventoryCommitFailedEvent event =
                new InventoryCommitFailedEvent(
                        orderId,
                        "ERROR",
                        "corr",
                        UUID.randomUUID()
                );

        when(inboxRepository.insertIfNotExists(event.messageId()))
                .thenReturn(1);

        when(workflowService.updateStatus(
                eq(orderId),
                eq(OrderStatus.INVENTORY_COMMIT_FAILED),
                any(OrderTransitionContext.class)
        ))
                .thenReturn(order);
        when(workflowService.updateStatus(
                eq(orderId),
                eq(OrderStatus.FAILED),
                any(OrderTransitionContext.class)
        ))
                .thenReturn(order);

        // WHEN
        orderService.handleInventoryCommitFailed(event);

        // THEN
        verify(workflowService)
                .updateStatus(
                        eq(orderId),
                        eq(OrderStatus.INVENTORY_COMMIT_FAILED),
                        any(OrderTransitionContext.class)
                );
        verify(workflowService)
                .updateStatus(
                        eq(orderId),
                        eq(OrderStatus.FAILED),
                        any(OrderTransitionContext.class)
                );

        verify(outboxService, times(2)).storeEvent(
                eq(orderId),
                eq("ORDER"),
                anyString(),
                any()
        );
    }
}
