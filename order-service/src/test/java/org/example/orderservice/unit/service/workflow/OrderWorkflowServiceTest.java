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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void should_mark_order_as_inventory_processing() {

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
        Order result = service.markInventoryProcessing(orderId);

        // THEN
        assertEquals(OrderStatus.INVENTORY_PROCESSING, result.getStatus());

        verify(repository).findById(orderId);
        verify(repository).save(order);
    }


    @Test
    void should_mark_order_as_failed() {

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
        Order result = service.markFailed(orderId);

        // THEN
        assertEquals(OrderStatus.FAILED, result.getStatus());

        verify(repository).findById(orderId);
        verify(repository).save(order);
    }


    @Test
    void should_mark_order_as_completed() {

        // GIVEN
        Long orderId = 1L;

        Order order = Order.builder()
                .id(orderId)
                .status(OrderStatus.INVENTORY_PROCESSING)
                .build();

        when(repository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(repository.save(order))
                .thenReturn(order);

        // WHEN
        Order result = service.markCompleted(orderId);

        // THEN
        assertEquals(OrderStatus.COMPLETED, result.getStatus());

        verify(repository).findById(orderId);
        verify(repository).save(order);
    }


    @Test
    void should_throw_when_order_not_found_for_inventory_processing() {

        // GIVEN
        Long orderId = 999L;

        when(repository.findById(orderId))
                .thenReturn(Optional.empty());

        // WHEN + THEN
        assertThrows(
                NoSuchElementException.class,
                () -> service.markInventoryProcessing(orderId)
        );

        verify(repository).findById(orderId);
        verify(repository, never()).save(any());
    }


    @Test
    void should_throw_when_order_not_found_for_failed_status() {

        // GIVEN
        Long orderId = 999L;

        when(repository.findById(orderId))
                .thenReturn(Optional.empty());

        // WHEN + THEN
        assertThrows(
                NoSuchElementException.class,
                () -> service.markFailed(orderId)
        );

        verify(repository).findById(orderId);
        verify(repository, never()).save(any());
    }

    @Test
    void should_throw_when_order_not_found_for_completed_status() {

        // GIVEN
        Long orderId = 999L;

        when(repository.findById(orderId))
                .thenReturn(Optional.empty());

        // WHEN + THEN
        assertThrows(
                NoSuchElementException.class,
                () -> service.markCompleted(orderId)
        );

        verify(repository).findById(orderId);
        verify(repository, never()).save(any());
    }


}
