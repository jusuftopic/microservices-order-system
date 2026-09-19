package org.example.paymentservice.service.provider.clients;

import org.example.paymentservice.dto.RefundRequest;
import org.example.paymentservice.dto.RefundResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockPaymentClientTest {

    private final MockPaymentClient client = new MockPaymentClient();

    @Test
    void shouldReturnSameRefundForRepeatedIdempotencyKey() {
        RefundRequest request = new RefundRequest(
                21L,
                11L,
                "mock-payment-11",
                null,
                "refund-key-21"
        );

        RefundResult first = client.refund(request);
        RefundResult repeated = client.refund(request);

        assertThat(first.succeeded()).isTrue();
        assertThat(first.providerRefundId()).isEqualTo("mock-refund-refund-key-21");
        assertThat(repeated).isEqualTo(first);
    }
}
