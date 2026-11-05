package org.example.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.orderservice.authorization.OwnershipPermissionService;
import org.example.orderservice.config.security.OrderSecurityConfig;
import org.example.orderservice.controller.OrderController;
import org.example.orderservice.dto.request.OrderItemRequest;
import org.example.orderservice.dto.request.OrderRequest;
import org.example.orderservice.dto.response.OrderItemResponse;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.exception.handler.GlobalExceptionHandler;
import org.example.orderservice.service.OrderService;
import org.example.orderservice.validation.OwnershipAccessValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the Order controller HTTP contract and its security wiring in an MVC slice.
 */
@WebMvcTest(OrderController.class)
@ContextConfiguration(classes = {
        OrderController.class,
        OrderSecurityConfig.class,
        OwnershipAccessValidator.class,
        GlobalExceptionHandler.class
})
class OrderControllerTest {

    private static final long ORDER_ID = 42L;
    private static final UUID CUSTOMER_ID =
            UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final LocalDateTime CREATED_AT =
            LocalDateTime.of(2026, 11, 3, 10, 15);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OwnershipPermissionService ownershipPermissionService;

    /**
     * Allows ownership by default so each test can focus on its declared concern.
     */
    @BeforeEach
    void allowOwnership() {
        when(ownershipPermissionService.canAccess(
                anyLong(),
                nullable(String.class)
        )).thenReturn(true);
    }

    /**
     * Returns HTTP 201 and delegates creation with the customer identity from the JWT.
     */
    @Test
    void createsOrderSuccessfully() throws Exception {
        OrderRequest request = validRequest();
        OrderResponse response = successfulResponse();
        when(orderService.createOrder(CUSTOMER_ID, request)).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .with(writeToken())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ORDER_ID))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.customerEmail").value("owner@example.com"))
                .andExpect(jsonPath("$.items[0].productId").value(7))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.submitted").value(true));

        verify(orderService).createOrder(CUSTOMER_ID, request);
    }

    /**
     * Rejects an unauthenticated order creation request.
     */
    @Test
    @WithAnonymousUser
    void rejectsUnauthenticatedCreateOrder() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }

    /**
     * Rejects order creation when the token lacks the write scope.
     */
    @Test
    void rejectsCreateOrderWithoutWriteScope() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(tokenWithoutAuthority())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    /**
     * Rejects an empty creation request before invoking the application service.
     */
    @Test
    void rejectsEmptyCreateOrderRequest() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .with(writeToken())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(orderService);
    }

    /**
     * Returns HTTP 500 with the failed submission response when persistence fails.
     */
    @Test
    void returnsInternalServerErrorForFailedSubmission() throws Exception {
        OrderRequest request = validRequest();
        OrderResponse response = new OrderResponse(
                null,
                null,
                request.customerEmail(),
                List.of(new OrderItemResponse(7L, 2)),
                request.description(),
                null,
                false
        );
        when(orderService.createOrder(CUSTOMER_ID, request)).thenReturn(response);

        mockMvc.perform(post("/api/v1/orders")
                        .with(writeToken())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.submitted").value(false));

        verify(orderService).createOrder(CUSTOMER_ID, request);
    }

    /**
     * Rejects creation when the authenticated JWT has no customer identifier.
     */
    @Test
    void rejectsCreateOrderWithoutCustomerIdentity() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("SCOPE_orders.write")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value("Access to this resource is forbidden"));

        verifyNoInteractions(orderService);
    }

    /**
     * Returns an order when scope and ownership checks allow access.
     */
    @Test
    void getsOrderSuccessfully() throws Exception {
        OrderResponse response = successfulResponse();
        when(orderService.getOrder(ORDER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/{orderId}", ORDER_ID)
                        .with(readToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.submitted").value(true));

        verify(ownershipPermissionService).canAccess(ORDER_ID, CUSTOMER_ID.toString());
        verify(orderService).getOrder(ORDER_ID);
    }

    /**
     * Rejects unauthenticated order retrieval.
     */
    @Test
    @WithAnonymousUser
    void rejectsUnauthenticatedGetOrder() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{orderId}", ORDER_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }

    /**
     * Rejects order retrieval when the token lacks the read scope.
     */
    @Test
    void rejectsGetOrderWithoutReadScope() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{orderId}", ORDER_ID)
                        .with(tokenWithoutAuthority()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    /**
     * Rejects a scoped caller when the ownership decision denies access.
     */
    @Test
    void rejectsGetOrderForNonOwner() throws Exception {
        when(ownershipPermissionService.canAccess(
                ORDER_ID,
                CUSTOMER_ID.toString()
        )).thenReturn(false);

        mockMvc.perform(get("/api/v1/orders/{orderId}", ORDER_ID)
                        .with(readToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value("Access to this resource is forbidden"));

        verify(ownershipPermissionService)
                .canAccess(ORDER_ID, CUSTOMER_ID.toString());
        verify(orderService, never()).getOrder(ORDER_ID);

        verifyNoMoreInteractions(ownershipPermissionService, orderService);
    }

    /**
     * Builds a valid creation request used by controller examples.
     *
     * @return valid order request
     */
    private OrderRequest validRequest() {
        return new OrderRequest(
                "owner@example.com",
                List.of(new OrderItemRequest(7L, 2)),
                "Test order"
        );
    }

    /**
     * Builds a successful order response used by controller examples.
     *
     * @return successful order response
     */
    private OrderResponse successfulResponse() {
        return new OrderResponse(
                ORDER_ID,
                "CREATED",
                "owner@example.com",
                List.of(new OrderItemResponse(7L, 2)),
                "Test order",
                CREATED_AT,
                true
        );
    }

    /**
     * Creates a JWT request processor with the Order write scope.
     *
     * @return configured JWT request processor
     */
    private RequestPostProcessor writeToken() {
        return tokenWithAuthority("SCOPE_orders.write");
    }

    /**
     * Creates a JWT request processor with the Order read scope.
     *
     * @return configured JWT request processor
     */
    private RequestPostProcessor readToken() {
        return tokenWithAuthority("SCOPE_orders.read");
    }

    /**
     * Creates an authenticated JWT carrying the application customer identifier.
     *
     * @param authority OAuth scope authority
     * @return configured JWT request processor
     */
    private RequestPostProcessor tokenWithAuthority(String authority) {
        return jwt()
                .jwt(token -> token.claim("customer_id", CUSTOMER_ID.toString()))
                .authorities(new SimpleGrantedAuthority(authority));
    }

    private RequestPostProcessor tokenWithoutAuthority() {
        return jwt()
                .jwt(token -> token.claim("customer_id", CUSTOMER_ID.toString()));
    }
}
