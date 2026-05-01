package org.example.orderservice.common.facade.payment;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Primary
public class RestPaymentFacade implements PaymentFacade {

    private final RestClient restClient;

    public RestPaymentFacade(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("http://localhost:8082/api/v1")
                .build();
    }

    @Override
    public void initiatePayment(Long orderId) {
        restClient.post()
                .uri("/payments/initiate")
                .body(orderId)
                .retrieve()
                .toBodilessEntity();
    }
}
