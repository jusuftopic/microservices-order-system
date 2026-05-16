package org.example.orderservice.mapper;


import org.example.orderservice.dto.request.OrderRequest;
import org.example.orderservice.dto.response.OrderItemResponse;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.entity.OrderItem;
import org.example.orderservice.enums.OrderStatus;

import java.util.List;

/**
 * Maps Order entity and DTOs.
 */
public class OrderMapper {

    public static Order toEntity(OrderRequest request) {
        Order order = Order.builder()
                .customerEmail(request.customerEmail())
                .description(request.description())
                .status(OrderStatus.CREATED)
                .build();


        request.items().forEach(itemRequest ->
                order.addItem(
                        OrderItem.builder()
                                .productId(itemRequest.productId())
                                .quantity(itemRequest.quantity())
                                .build()
                )
        );

        return order;
    }

    public static OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(),
                        item.getQuantity()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getCustomerEmail(),
                items,
                order.getDescription(),
                order.getCreatedAt()
        );
    }
}
