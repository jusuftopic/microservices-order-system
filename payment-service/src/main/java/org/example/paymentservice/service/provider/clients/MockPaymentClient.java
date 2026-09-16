package org.example.paymentservice.service.provider.clients;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.dto.PaymentRequest;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.enums.PaymentProviderStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock Payment Provider simulates payment process to the 3rd party system
 */
@Service
@ConditionalOnProperty(
        name = "app.payment.provider",
        havingValue = "mock",
        matchIfMissing = true
)
@Slf4j
@RequiredArgsConstructor
public class MockPaymentClient implements PaymentClient {

    private final Map<String, PaymentResultDTO> processed = new ConcurrentHashMap<>();

    @Setter
    private volatile Boolean forceSuccess = false;


    @Override
    public PaymentResultDTO pay(PaymentRequest request) {
        // simulate provider-side idempotency
        if (processed.containsKey(request.idempotencyKey())) {
            log.info("[PAYMENT-PROVIDER][MOCK] Returning cached result for key {}", request.idempotencyKey());
            return processed.get(request.idempotencyKey());
        }

        // simulate randomness like real systems
        boolean success = forceSuccess || Math.random() > 0.2;
        PaymentResultDTO result;

        if (success) {
            result = new PaymentResultDTO(
                    PaymentProviderStatus.SUCCEEDED,
                    "mock-" + request.idempotencyKey(),
                    null,
                    null,
                    "MOCK"
            );
        }
        else {
            result = new PaymentResultDTO(
                    PaymentProviderStatus.FAILED,
                    null,
                    "INSUFFICIENT_FUNDS",
                    null,
                    "MOCK"
            );
        }
        processed.put(request.idempotencyKey(), result);
        return result;

    }

    public void resetCache() {
        processed.clear();
    }
}
