package org.example.orderservice.mapper;


import org.example.orderservice.dto.request.OrderRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.enums.OrderStatus;

/**
 * Maps Order entity and DTOs.
 */
public class OrderMapper {

    public static Order toEntity(OrderRequest request) {
        return Order.builder()
                .customerEmail(request.customerEmail())
                .amount(request.amount())
                .description(request.description())
                .status(OrderStatus.CREATED)
                .build();
    }

    public static OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getCustomerEmail(),
                order.getAmount(),
                order.getDescription(),
                order.getCreatedAt()
        );
    }
}
