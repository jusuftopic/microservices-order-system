package org.example.orderservice.integration;

import org.example.messagingstarter.contracts.lifecycle.OrderStatus;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.Order;
import org.example.orderservice.repository.OrderRepository;
import org.example.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies JWT scope and persisted ownership enforcement across the Order HTTP boundary.
 */
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "spring.datasource.url=jdbc:h2:mem:order-ownership-it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
class OrderOwnershipEndpointSecurityIT {

    private static final UUID OWNER_ID =
            UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID OTHER_CUSTOMER_ID =
            UUID.fromString("33333333-3333-4333-8333-333333333333");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private OrderService orderService;

    private Long orderId;

    /**
     * Persists one owned order and configures its lookup response.
     */
    @BeforeEach
    void setUpOwnedOrder() {
        Order order = orderRepository.saveAndFlush(Order.builder()
                .status(OrderStatus.CREATED)
                .customerEmail("owner@example.com")
                .customerId(OWNER_ID)
                .correlationId("correlation-42")
                .description("owned order")
                .build());
        orderId = order.getId();

        when(orderService.getOrder(orderId)).thenReturn(new OrderResponse(
                orderId,
                OrderStatus.CREATED.name(),
                "owner@example.com",
                List.of(),
                "owned order",
                LocalDateTime.now(),
                true
        ));
    }

    /**
     * Allows a scoped customer to read their own order.
     */
    @Test
    void allowsOwnerToReadOrder() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .with(readToken(OWNER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId));

        verify(orderService).getOrder(orderId);
    }

    /**
     * Rejects a scoped customer attempting to read another customer's order.
     */
    @Test
    void deniesNonOwnerFromReadingOrder() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .with(readToken(OTHER_CUSTOMER_ID)))
                .andExpect(status().isForbidden());

        verify(orderService, never()).getOrder(orderId);
    }

    /**
     * Creates a JWT request processor with the Order read scope.
     *
     * @param customerId internal customer identifier
     * @return configured JWT request processor
     */
    private org.springframework.test.web.servlet.request.RequestPostProcessor readToken(
            UUID customerId
    ) {
        return jwt()
                .jwt(token -> token.claim("customer_id", customerId.toString()))
                .authorities(new SimpleGrantedAuthority("SCOPE_orders.read"));
    }
}
