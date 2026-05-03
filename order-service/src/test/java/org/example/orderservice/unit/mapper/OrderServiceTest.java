package org.example.orderservice.unit.mapper;

import org.example.orderservice.dto.request.OrderRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.mapper.OrderMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link OrderMapper}
 */
public class OrderServiceTest {

    @Test
    void should_map_request_to_entity() {

        // GIVEN
        OrderRequest request = new OrderRequest(
                "test@example.com",
                BigDecimal.valueOf(100),
                "Test order"
        );

        // WHEN
        Order result = OrderMapper.toEntity(request);

        // THEN
        assertNotNull(result);
        assertEquals("test@example.com", result.getCustomerEmail());
        assertEquals(BigDecimal.valueOf(100), result.getAmount());
        assertEquals("Test order", result.getDescription());

        // important default
        assertEquals(OrderStatus.CREATED, result.getStatus());

        // id + createdAt should not be set yet
        assertNull(result.getId());
        assertNull(result.getCreatedAt());
    }

    @Test
    void should_map_entity_to_response() {

        // GIVEN
        Order order = Order.builder()
                .id(1L)
                .customerEmail("test@example.com")
                .amount(BigDecimal.valueOf(200))
                .description("Order desc")
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();

        // WHEN
        OrderResponse response = OrderMapper.toResponse(order);

        // THEN
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("CREATED", response.status());
        assertEquals("test@example.com", response.customerEmail());
        assertEquals(BigDecimal.valueOf(200), response.amount());
        assertEquals("Order desc", response.description());
        assertEquals(order.getCreatedAt(), response.createdAt());
    }
}
