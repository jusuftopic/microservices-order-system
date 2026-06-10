package org.example.orderservice.unit.service.workflow;

import org.example.orderservice.entity.Order;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.service.workflow.OrderWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

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

    private OrderWorkflowService service;

    @BeforeEach
    void setUp() {
        service = new OrderWorkflowService(repository);
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
    }

}
