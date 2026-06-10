package org.example.orderservice.integration;

import org.example.orderservice.dto.request.OrderItemRequest;
import org.example.orderservice.dto.request.OrderRequest;
import org.example.orderservice.dto.response.OrderResponse;
import org.example.orderservice.entity.OutboxEvent;
import org.example.orderservice.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests integration flow of creating an order and store outbox pattern
 */
public class OrderFlowIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    public void setUp() {
        outboxRepository.deleteAll();
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
        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                "/api/v1/orders",
                request,
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


        List<OutboxEvent> events = outboxRepository.findAll();

        assertThat(events).hasSize(1);

        OutboxEvent event = events.get(0);

        assertThat(event.getAggregateId()).isEqualTo(orderId);
        assertThat(event.getProcessed()).isFalse();
    }
}
