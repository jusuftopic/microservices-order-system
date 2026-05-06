package org.example.paymentservice.integration;

import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.service.provider.PaymentProviderWrapper;
import org.example.paymentservice.service.provider.clients.MockPaymentClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentProviderIT extends AbstractIntegrationTest {

    @Autowired
    private PaymentProviderWrapper paymentProvider;

    @Autowired
    private MockPaymentClient mockPaymentClient;

    @Test
    void shouldReturnSuccessfulPayment() {

        // given
        mockPaymentClient.setForceSuccess(true);

        // when
        PaymentResultDTO result = paymentProvider.pay(
                1L,
                UUID.randomUUID()
        );

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.transactionId()).isNotNull();
    }

    @Test
    void shouldReturnFailedPayment() {
        // given
        mockPaymentClient.setForceSuccess(false);

        // when
        PaymentResultDTO result = paymentProvider.pay(
                1L,
                UUID.randomUUID()
        );

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo("INSUFFICIENT_FUNDS");
    }
}
