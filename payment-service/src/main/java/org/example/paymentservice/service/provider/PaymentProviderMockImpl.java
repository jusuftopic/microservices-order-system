package org.example.paymentservice.service.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock Payment Provider simulates payment process to the 3rd party system
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentProviderMockImpl implements PaymentProvider {


    @Override
    public PaymentResultDTO pay(Long orderId) {
        // simulate randomness like real systems
        boolean success = Math.random() > 0.2;

        if (success) {
            return new PaymentResultDTO(
                    true,
                    UUID.randomUUID().toString(),
                    null
            );
        }

        return new PaymentResultDTO(
                false,
                null,
                "INSUFFICIENT_FUNDS"
        );
    }
}
