package org.example.orderservice.unit.service.workflow;

import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.events.OrderLifecycleTransitionedEvent;
import org.example.messagingstarter.contracts.lifecycle.LifecycleReasonCode;
import org.example.messagingstarter.contracts.lifecycle.LifecycleTrigger;
import org.example.messagingstarter.contracts.lifecycle.OrchestrationDecisionCode;
import org.example.orderservice.entity.Order;
import org.example.messagingstarter.contracts.lifecycle.OrderStatus;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.service.outbox.OrderOutboxService;
import org.example.orderservice.service.workflow.OrderWorkflowService;
import org.example.orderservice.service.workflow.OrderTransitionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * Tests {@link OrderWorkflowService}
 */
@ExtendWith(MockitoExtension.class)
public class OrderWorkflowServiceTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private OrderOutboxService outboxService;

    private OrderWorkflowService service;

    @BeforeEach
    void setUp() {
        service = new OrderWorkflowService(repository, outboxService);
    }

    @Test
    void should_update_status_from_created_to_inventory_reserve_completed() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.CREATED)
                .build();

        when(repository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(repository.save(order))
                .thenReturn(order);

        // WHEN
        Order result = service.updateStatus(
                orderId,
                OrderStatus.INVENTORY_RESERVE_COMPLETED
        );

        // THEN
        assertEquals(
                OrderStatus.INVENTORY_RESERVE_COMPLETED,
                result.getStatus()
        );

        verify(repository).findById(orderId);
        verify(repository).save(order);
    }

    @Test
    void should_store_lifecycle_evidence_in_same_transition() {

        Long orderId = 1L;
        UUID sourceEventId = UUID.randomUUID();
        UUID paymentCommandId = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.CREATED)
                .correlationId("corr-123")
                .build();

        when(repository.findById(orderId)).thenReturn(Optional.of(order));
        when(repository.save(order)).thenReturn(order);

        service.updateStatus(
                orderId,
                OrderStatus.INVENTORY_RESERVE_COMPLETED,
                OrderTransitionContext.causedBy(
                                LifecycleReasonCode.INVENTORY_RESERVED,
                                LifecycleTrigger.INVENTORY_RESERVED,
                                sourceEventId
                        )
                        .withDecision(OrchestrationDecisionCode.PROCESS_PAYMENT, paymentCommandId)
        );

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(outboxService).storeEvent(
                eq(orderId),
                eq("ORDER"),
                eq(EventConstants.EVENT_ORDER_LIFECYCLE_TRANSITIONED),
                payload.capture()
        );

        OrderLifecycleTransitionedEvent lifecycleEvent =
                assertInstanceOf(OrderLifecycleTransitionedEvent.class, payload.getValue());

        assertEquals("CREATED", lifecycleEvent.previousStatus());
        assertEquals("INVENTORY_RESERVE_COMPLETED", lifecycleEvent.newStatus());
        assertEquals("INVENTORY_RESERVED", lifecycleEvent.reasonCode());
        assertEquals("INVENTORY_SERVICE", lifecycleEvent.sourceService());
        assertEquals(sourceEventId, lifecycleEvent.causationId());
        assertEquals("PROCESS_PAYMENT", lifecycleEvent.orchestrationDecision().code());
        assertEquals("PAYMENT_SERVICE",
                lifecycleEvent.orchestrationDecision().targetService());
        assertEquals(paymentCommandId, lifecycleEvent.orchestrationDecision().commandId());
        assertEquals("corr-123", lifecycleEvent.correlationId());
        assertFalse(lifecycleEvent.compensation().required());
        assertNotNull(lifecycleEvent.occurredAt());
        assertNotNull(lifecycleEvent.messageId());
        assertEquals(1, lifecycleEvent.eventVersion());
    }

    @Test
    void should_update_status_from_inventory_reserved_to_payment_completed() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.INVENTORY_RESERVE_COMPLETED)
                .build();

        when(repository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(repository.save(order))
                .thenReturn(order);

        // WHEN
        Order result = service.updateStatus(
                orderId,
                OrderStatus.PAYMENT_COMPLETED
        );

        // THEN
        assertEquals(
                OrderStatus.PAYMENT_COMPLETED,
                result.getStatus()
        );

        verify(repository).save(order);
    }

    @Test
    void should_update_status_from_payment_completed_to_inventory_commit_completed() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.PAYMENT_COMPLETED)
                .build();

        when(repository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(repository.save(order))
                .thenReturn(order);

        // WHEN
        Order result = service.updateStatus(
                orderId,
                OrderStatus.INVENTORY_COMMIT_COMPLETED
        );

        // THEN
        assertEquals(
                OrderStatus.INVENTORY_COMMIT_COMPLETED,
                result.getStatus()
        );
    }

    @Test
    void should_update_status_from_inventory_commit_completed_to_completed() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.INVENTORY_COMMIT_COMPLETED)
                .build();

        when(repository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(repository.save(order))
                .thenReturn(order);

        // WHEN
        Order result = service.updateStatus(
                orderId,
                OrderStatus.COMPLETED
        );

        // THEN
        assertEquals(
                OrderStatus.COMPLETED,
                result.getStatus()
        );
    }

    @Test
    void should_transition_to_failed_after_inventory_failed() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.INVENTORY_RESERVE_FAILED)
                .build();

        when(repository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(repository.save(order))
                .thenReturn(order);

        // WHEN
        Order result = service.updateStatus(
                orderId,
                OrderStatus.FAILED
        );

        // THEN
        assertEquals(OrderStatus.FAILED, result.getStatus());
    }

    @Test
    void should_transition_to_failed_after_payment_failed() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.PAYMENT_FAILED)
                .build();

        when(repository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(repository.save(order))
                .thenReturn(order);

        // WHEN
        Order result = service.updateStatus(
                orderId,
                OrderStatus.FAILED
        );

        // THEN
        assertEquals(OrderStatus.FAILED, result.getStatus());
    }

    @Test
    void should_throw_when_transition_is_invalid() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.CREATED)
                .build();

        when(repository.findById(orderId))
                .thenReturn(Optional.of(order));

        // WHEN + THEN
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.updateStatus(
                        orderId,
                        OrderStatus.COMPLETED
                )
        );

        assertTrue(
                ex.getMessage().contains(
                        "Invalid order transition"
                )
        );

        verify(repository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void should_throw_when_order_not_found() {

        // GIVEN
        Long orderId = 999L;

        when(repository.findById(orderId))
                .thenReturn(Optional.empty());

        // WHEN + THEN
        assertThrows(
                NoSuchElementException.class,
                () -> service.updateStatus(
                        orderId,
                        OrderStatus.COMPLETED
                )
        );

        verify(repository).findById(orderId);
        verify(repository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

}
