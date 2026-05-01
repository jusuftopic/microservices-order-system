package org.example.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.orderservice.dto.request.OrderRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.entity.OutboxEvent;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

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
    private OutboxRepository outboxRepository;

    @Mock
    private ObjectMapper objectMapper;

    /* class under test */
    private OrderService orderService;

    @BeforeEach
    public void setUp() {
        orderService = new OrderService(
                repository, outboxRepository, objectMapper
        );
    }

    @Test
    void should_create_order_and_store_outbox_event() throws Exception {

        // GIVEN
        OrderRequest request = new OrderRequest(
                "test@mail.com",
                BigDecimal.valueOf(100),
                "desc"
        );

        Order savedOrder = Order.builder()
                .id(1L)
                .customerEmail("test@mail.com")
                .amount(BigDecimal.valueOf(100))
                .description("desc")
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        when(repository.save(any(Order.class))).thenReturn(savedOrder);
        when(objectMapper.writeValueAsString(any())).thenReturn("{json}");

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);

        // WHEN
        OrderResponse response = orderService.createOrder(request);

        // THEN
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("CREATED", response.status());

        verify(repository).save(any(Order.class));
        verify(outboxRepository).save(outboxCaptor.capture());

        OutboxEvent outbox = outboxCaptor.getValue();

        assertNotNull(outbox.getId());
        assertEquals("ORDER", outbox.getAggregateType());
        assertEquals(1L, outbox.getAggregateId());
        assertEquals("PAYMENT_REQUESTED", outbox.getEventType());
        assertEquals("{json}", outbox.getPayload());
        assertFalse(outbox.getProcessed());
        assertNotNull(outbox.getCreatedAt());
    }

    @Test
    void should_throw_when_serialization_fails() throws Exception {

        // GIVEN
        OrderRequest request = new OrderRequest(
                "fail@mail.com",
                BigDecimal.valueOf(50),
                "desc"
        );

        Order savedOrder = Order.builder()
                .id(1L)
                .status(OrderStatus.CREATED)
                .build();

        when(repository.save(any())).thenReturn(savedOrder);

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("fail") {});

        // WHEN + THEN
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> orderService.createOrder(request)
        );

        assertTrue(ex.getMessage().contains("Failed to serialize outbox payload"));

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void should_return_order_when_found() {

        // GIVEN
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.CREATED)
                .customerEmail("test@mail.com")
                .amount(BigDecimal.TEN)
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
}
