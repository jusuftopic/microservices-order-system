package org.example.orderservice.mapper;


import org.example.orderservice.dto.request.OrderRequest;
import org.example.orderservice.dto.response.OrderItemResponse;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.entity.OrderItem;
import org.example.messagingstarter.contracts.lifecycle.OrderStatus;

import java.util.List;
import java.util.UUID;

/**
 * Maps Order entity and DTOs.
 */
public class OrderMapper {

    /**
     * Maps an authenticated customer's request to a new order entity.
     *
     * @param customerId internal identifier of the customer that owns the order
     * @param request order creation request
     * @return new order entity
     */
    public static Order toEntity(UUID customerId, OrderRequest request) {
        Order order = Order.builder()
                .customerId(customerId)
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
                order.getCreatedAt(),
                true
        );
    }

    /**
     * Creates a response indicating that an order request was not persisted.
     *
     * @param request rejected order request
     * @return response without an order identifier or lifecycle status
     */
    public static OrderResponse toSubmissionFailureResponse(OrderRequest request) {
        List<OrderItemResponse> items = request.items().stream()
                .map(item -> new OrderItemResponse(item.productId(), item.quantity()))
                .toList();

        return new OrderResponse(null, null, request.customerEmail(), items, request.description(), null, false);
    }
}
