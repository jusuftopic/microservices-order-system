package org.example.paymentservice.service.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.service.provider.clients.PaymentClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Payment provider wrapper wraps client call with resiliency measures
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentProviderWrapper {

    private final PaymentClient paymentClient;

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(
            name = "payment",
            fallbackMethod = "fallback"
    )
    @io.github.resilience4j.retry.annotation.Retry(name = "payment")
    public PaymentResultDTO pay(Long orderId, String idempotencyKey) {
       return paymentClient.pay(orderId, idempotencyKey);
    }

    /**
     * Fallback is triggered when:
     * - circuit breaker is OPEN
     * - retries exhausted
     * - timeout occurs
     */
    private PaymentResultDTO fallback(Long orderId, String idempotencyKey,
                                      Throwable ex) {
        log.warn(
                "[PAYMENT-PROVIDER] fallback triggered for orderId={}.",
                orderId, ex
        );

        return new PaymentResultDTO(
                false,
                null,
                "PAYMENT_SERVICE_UNAVAILABLE",
                "MOCK"
        );
    }
}
