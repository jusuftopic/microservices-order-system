package org.example.orderservice.integration;

import org.example.messagingstarter.outbox.entity.OutboxEvent;
import org.example.orderservice.dto.request.OrderItemRequest;
import org.example.orderservice.dto.request.OrderRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.messagingstarter.outbox.repository.OutboxRepository;
import org.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests integration flow of creating an order and store outbox pattern
 */
public class OrderFlowIT extends AbstractIntegrationTest {

    private static final String ACCESS_TOKEN = "order-flow-token";
    private static final UUID CUSTOMER_ID =
            UUID.fromString("22222222-2222-4222-8222-222222222222");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    public void setUp() {
        outboxRepository.deleteAll();
        orderRepository.deleteAll();

        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue(ACCESS_TOKEN)
                .header("alg", "RS256")
                .subject("11111111-1111-4111-8111-111111111111")
                .claim("scope", "orders.write")
                .claim("customer_id", CUSTOMER_ID.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
        given(jwtDecoder.decode(ACCESS_TOKEN)).willReturn(jwt);
    }

    @Test
    void shouldCreateOrderAndGenerateOutboxEvent() {

        // given
        OrderRequest request = new OrderRequest(
                "test@test.com",
                List.of(
                        new OrderItemRequest(1L, 2),
                        new OrderItemRequest(2L, 1)
                ),
                "test order"
        );

        // when
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ACCESS_TOKEN);

        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                OrderResponse.class
        );

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        OrderResponse responseBody = response.getBody();
        assertThat(responseBody).isNotNull();

        Long orderId = responseBody.id();

        assertThat(responseBody.items()).hasSize(2);
        assertThat(responseBody.items().get(0).productId()).isEqualTo(1L);
        assertThat(responseBody.items().get(0).quantity()).isEqualTo(2);
        assertThat(orderRepository.findById(orderId).orElseThrow().getCustomerId())
                .isEqualTo(CUSTOMER_ID);


        List<OutboxEvent> events = outboxRepository.findAll();

        assertThat(events).hasSize(1);

        OutboxEvent event = events.get(0);

        assertThat(event.getAggregateId()).isEqualTo(orderId);
        assertThat(event.getProcessed()).isFalse();
    }
}
