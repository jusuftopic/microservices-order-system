package org.example.paymentservice.service;

import org.example.paymentservice.enums.PaymentProviderStatus;
import org.example.paymentservice.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatusTransitionPolicyTest {

    private final PaymentStatusTransitionPolicy policy =
            new PaymentStatusTransitionPolicy();

    @Test
    void mapsProcessingPaymentToProviderTerminalOutcome() {
        assertThat(policy.targetStatus(
                PaymentStatus.PROCESSING,
                PaymentProviderStatus.SUCCEEDED
        )).isEqualTo(PaymentStatus.SUCCESS);

        assertThat(policy.targetStatus(
                PaymentStatus.PROCESSING,
                PaymentProviderStatus.FAILED
        )).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void neverRegressesTerminalPayment() {
        assertThat(policy.targetStatus(
                PaymentStatus.SUCCESS,
                PaymentProviderStatus.PROCESSING
        )).isEqualTo(PaymentStatus.SUCCESS);

        assertThat(policy.targetStatus(
                PaymentStatus.FAILED,
                PaymentProviderStatus.SUCCEEDED
        )).isEqualTo(PaymentStatus.FAILED);
    }
}
