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
import org.example.orderservice.service.workflow.OrderTransitionResult;
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

        when(repository.findByIdForUpdate(orderId))
                .thenReturn(Optional.of(order));

        when(repository.save(order))
                .thenReturn(order);

        // WHEN
        OrderTransitionResult result = transition(orderId,
                OrderStatus.INVENTORY_RESERVE_COMPLETED);

        // THEN
        assertEquals(
                OrderStatus.INVENTORY_RESERVE_COMPLETED,
                result.order().getStatus()
        );

        verify(repository).findByIdForUpdate(orderId);
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

        when(repository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(repository.save(order)).thenReturn(order);

        service.tryUpdateStatus(
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

        when(repository.findByIdForUpdate(orderId))
                .thenReturn(Optional.of(order));

        when(repository.save(order))
                .thenReturn(order);

        // WHEN
        OrderTransitionResult result = transition(orderId, OrderStatus.PAYMENT_COMPLETED);

        // THEN
        assertEquals(
                OrderStatus.PAYMENT_COMPLETED,
                result.order().getStatus()
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

        when(repository.findByIdForUpdate(orderId))
                .thenReturn(Optional.of(order));

        when(repository.save(order))
                .thenReturn(order);

        // WHEN
        OrderTransitionResult result = transition(orderId,
                OrderStatus.INVENTORY_COMMIT_COMPLETED);

        // THEN
        assertEquals(
                OrderStatus.INVENTORY_COMMIT_COMPLETED,
                result.order().getStatus()
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

        when(repository.findByIdForUpdate(orderId))
                .thenReturn(Optional.of(order));

        when(repository.save(order))
                .thenReturn(order);

        // WHEN
        OrderTransitionResult result = transition(orderId, OrderStatus.COMPLETED);

        // THEN
        assertEquals(
                OrderStatus.COMPLETED,
                result.order().getStatus()
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

        when(repository.findByIdForUpdate(orderId))
                .thenReturn(Optional.of(order));

        when(repository.save(order))
                .thenReturn(order);

        // WHEN
        OrderTransitionResult result = transition(orderId, OrderStatus.FAILED);

        // THEN
        assertEquals(OrderStatus.FAILED, result.order().getStatus());
    }

    @Test
    void should_transition_to_failed_after_payment_failed() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.PAYMENT_FAILED)
                .build();

        when(repository.findByIdForUpdate(orderId))
                .thenReturn(Optional.of(order));

        when(repository.save(order))
                .thenReturn(order);

        // WHEN
        OrderTransitionResult result = transition(orderId, OrderStatus.FAILED);

        // THEN
        assertEquals(OrderStatus.FAILED, result.order().getStatus());
    }

    @Test
    void should_throw_when_transition_is_invalid() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.CREATED)
                .build();

        when(repository.findByIdForUpdate(orderId))
                .thenReturn(Optional.of(order));

        // WHEN + THEN
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> transition(orderId, OrderStatus.COMPLETED)
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

        when(repository.findByIdForUpdate(orderId))
                .thenReturn(Optional.empty());

        // WHEN + THEN
        assertThrows(
                NoSuchElementException.class,
                () -> transition(orderId, OrderStatus.COMPLETED)
        );

        verify(repository).findByIdForUpdate(orderId);
        verify(repository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void should_report_that_transition_from_final_state_was_not_applied() {
        Long orderId = 1L;
        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.TIMED_OUT)
                .build();
        when(repository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        OrderTransitionResult result = service.tryUpdateStatus(
                orderId,
                OrderStatus.PAYMENT_COMPLETED,
                OrderTransitionContext.causedBy(
                        LifecycleReasonCode.PAYMENT_COMPLETED,
                        LifecycleTrigger.PAYMENT_COMPLETED,
                        UUID.randomUUID()
                )
        );

        assertFalse(result.transitioned());
        assertEquals(OrderStatus.TIMED_OUT, result.previousStatus());
        assertEquals(OrderStatus.TIMED_OUT, result.order().getStatus());
        verify(repository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void should_lock_order_before_timeout_transition() {
        // GIVEN
        Long orderId = 1L;
        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.CREATED)
                .correlationId("corr-123")
                .build();
        when(repository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        when(repository.save(order)).thenReturn(order);

        // WHEN
        OrderTransitionResult result = service.tryUpdateStatus(
                orderId,
                OrderStatus.TIMED_OUT,
                OrderTransitionContext.causedBy(
                        LifecycleReasonCode.ORDER_PROCESSING_TIMEOUT,
                        LifecycleTrigger.ORDER_TIMEOUT_DETECTED,
                        null
                )
        );

        // THEN
        assertTrue(result.transitioned());
        assertEquals(OrderStatus.CREATED, result.previousStatus());
        assertEquals(OrderStatus.TIMED_OUT, result.targetStatus());
        assertEquals(OrderStatus.TIMED_OUT, result.order().getStatus());
        verify(repository).findByIdForUpdate(orderId);
        verify(repository).save(order);
        verify(outboxService).storeEvent(
                eq(orderId),
                eq("ORDER"),
                eq(EventConstants.EVENT_ORDER_LIFECYCLE_TRANSITIONED),
                any(OrderLifecycleTransitionedEvent.class)
        );
    }

    @Test
    void should_not_apply_locked_timeout_transition_twice() {
        // GIVEN
        Long orderId = 1L;
        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.TIMED_OUT)
                .build();
        when(repository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        // WHEN
        OrderTransitionResult result = service.tryUpdateStatus(
                orderId,
                OrderStatus.TIMED_OUT,
                OrderTransitionContext.causedBy(
                        LifecycleReasonCode.ORDER_PROCESSING_TIMEOUT,
                        LifecycleTrigger.ORDER_TIMEOUT_DETECTED,
                        null
                )
        );

        // THEN
        assertFalse(result.transitioned());
        verify(repository).findByIdForUpdate(orderId);
        verify(repository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    private OrderTransitionResult transition(Long orderId, OrderStatus targetStatus) {
        return service.tryUpdateStatus(
                orderId,
                targetStatus,
                OrderTransitionContext.causedBy(
                        LifecycleReasonCode.ORDER_STATUS_UPDATED,
                        LifecycleTrigger.ORDER_STATUS_UPDATE,
                        null
                )
        );
    }

}
