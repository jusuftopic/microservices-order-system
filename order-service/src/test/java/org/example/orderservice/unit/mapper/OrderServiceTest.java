package org.example.orderservice.unit.mapper;

import org.example.orderservice.dto.request.OrderItemRequest;
import org.example.orderservice.dto.request.OrderRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.entity.OrderItem;
import org.example.orderservice.enums.OrderStatus;
import org.example.orderservice.mapper.OrderMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link OrderMapper}
 */
public class OrderServiceTest {

    @Test
    void should_map_request_to_entity() {

        // GIVEN
        OrderRequest request = new OrderRequest(
                "test1@example.com",
                List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(2L, 1)
                ),
                "Test order"
        );


        // WHEN
        Order result = OrderMapper.toEntity(request);

        // THEN
        assertNotNull(result);
        assertEquals("test1@example.com", result.getCustomerEmail());
        assertEquals("Test order", result.getDescription());

        // important default
        assertEquals(OrderStatus.CREATED, result.getStatus());


        // items
        assertNotNull(result.getItems());
        assertEquals(2, result.getItems().size());

        OrderItem firstItem = result.getItems().get(0);
        assertEquals(1L, firstItem.getProductId());
        assertEquals(2, firstItem.getQuantity());

        // test bidirectional relationship is set
        assertEquals(result, firstItem.getOrder());
    }

    @Test
    void should_map_entity_to_response() {

        // GIVEN
        Order order = Order.builder()
                .id(1L)
                .customerEmail("test@example.com")
                .description("Order desc")
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();


        OrderItem item1 = OrderItem.builder()
                .productId(10L)
                .quantity(3)
                .order(order)
                .build();

        OrderItem item2 = OrderItem.builder()
                .productId(20L)
                .quantity(1)
                .order(order)
                .build();

        order.setItems(List.of(item1, item2));

        // WHEN
        OrderResponse response = OrderMapper.toResponse(order);

        // THEN
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("CREATED", response.status());
        assertEquals("test@example.com", response.customerEmail());
        assertEquals("Order desc", response.description());
        assertEquals(order.getCreatedAt(), response.createdAt());


        assertNotNull(response.items());
        assertEquals(2, response.items().size());

        assertEquals(10L, response.items().get(0).productId());
        assertEquals(3, response.items().get(0).quantity());
    }
}
