package org.example.paymentservice.service.provider;

import org.example.paymentservice.dto.RefundRequest;
import org.example.paymentservice.enums.RefundProviderStatus;
import org.example.paymentservice.exception.PaymentProviderNonRetryableException;
import org.example.paymentservice.exception.PaymentProviderRetryableException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentProviderWrapperTest {

    private final PaymentProviderWrapper wrapper = new PaymentProviderWrapper(null);
    private final RefundRequest request = new RefundRequest(
            7L,
            3L,
            "provider-payment-7",
            null,
            "refund-key-7"
    );

    @Test
    void maps_exhausted_transient_failure_to_unknown_outcome() {
        var exception = new PaymentProviderRetryableException(
                "temporary failure",
                "STRIPE",
                "request-7",
                "rate_limit",
                null
        );

        var result = wrapper.refundFallback(request, exception);

        assertThat(result.status()).isEqualTo(RefundProviderStatus.OUTCOME_UNKNOWN);
        assertThat(result.failureReason()).isEqualTo("PAYMENT_PROVIDER_OUTCOME_UNKNOWN");
    }

    @Test
    void maps_definitive_provider_rejection_to_failed() {
        var exception = new PaymentProviderNonRetryableException(
                "request rejected",
                "STRIPE",
                "request-7",
                "invalid_request",
                null
        );

        var result = wrapper.refundFallback(request, exception);

        assertThat(result.status()).isEqualTo(RefundProviderStatus.FAILED);
        assertThat(result.failureReason()).isEqualTo("PAYMENT_PROVIDER_REQUEST_REJECTED");
    }

    @Test
    void maps_unclassified_failure_to_unknown_outcome() {
        var result = wrapper.refundFallback(
                request,
                new IllegalStateException("unexpected mapping failure")
        );

        assertThat(result.status()).isEqualTo(RefundProviderStatus.OUTCOME_UNKNOWN);
    }
}
