package org.example.paymentservice.service.provider.clients;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock Payment Provider simulates payment process to the 3rd party system
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MockPaymentClient implements PaymentClient {

    private final Map<UUID, PaymentResultDTO> processed = new ConcurrentHashMap<>();

    @Setter
    private volatile Boolean forceSuccess;


    @Override
    public PaymentResultDTO pay(Long orderId, UUID idempotencyKey) {
        // simulate provider-side idempotency
        if (processed.containsKey(idempotencyKey)) {
            log.info("[PAYMENT-PROVIDER][MOCK] Returning cached result for key {}", idempotencyKey);
            return processed.get(idempotencyKey);
        }

        // simulate randomness like real systems
        boolean success = forceSuccess || Math.random() > 0.2;
        PaymentResultDTO result;

        if (success) {
            result = new PaymentResultDTO(
                    true,
                    UUID.randomUUID().toString(),
                    null
            );
        }
        else {
            result = new PaymentResultDTO(
                    false,
                    null,
                    "INSUFFICIENT_FUNDS"
            );
        }
        processed.put(idempotencyKey, result);
        return result;

    }
}
