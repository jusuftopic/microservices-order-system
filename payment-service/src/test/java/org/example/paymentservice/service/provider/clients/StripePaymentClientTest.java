package org.example.paymentservice.service.provider.clients;

import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.ApiException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.RateLimitException;
import org.example.paymentservice.exception.PaymentProviderNonRetryableException;
import org.example.paymentservice.exception.PaymentProviderRetryableException;
import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class StripePaymentClientTest {

    @Test
    void classifiesConnectionFailureAsRetryable() {
        RuntimeException result = StripePaymentClient.translate(
                new ApiConnectionException("Connection failed")
        );

        assertThat(result).isInstanceOf(PaymentProviderRetryableException.class);
    }

    @Test
    void classifiesRateLimitAsRetryable() {
        RuntimeException result = StripePaymentClient.translate(
                new RateLimitException(
                        "Rate limited", null, "req-rate", "rate_limit", 429, null
                )
        );

        assertThat(result).isInstanceOf(PaymentProviderRetryableException.class);
        assertThat(((PaymentProviderRetryableException) result).getProviderRequestId())
                .isEqualTo("req-rate");
    }

    @Test
    void classifiesServerFailureAsRetryable() {
        RuntimeException result = StripePaymentClient.translate(
                new ApiException("Unavailable", "req-server", "api_error", 503, null)
        );

        assertThat(result).isInstanceOf(PaymentProviderRetryableException.class);
    }

    @Test
    void classifiesAuthenticationFailureAsNonRetryable() {
        RuntimeException result = StripePaymentClient.translate(
                new AuthenticationException(
                        "Invalid API key", "req-auth", "authentication_error", 401
                )
        );

        assertThat(result).isInstanceOf(PaymentProviderNonRetryableException.class);
        assertThat(((PaymentProviderNonRetryableException) result).getProviderRequestId())
                .isEqualTo("req-auth");
    }

    @Test
    void classifiesClientApiFailureAsNonRetryable() {
        RuntimeException result = StripePaymentClient.translate(
                new ApiException("Invalid request", "req-client", "invalid_request", 400, null)
        );

        assertThat(result).isInstanceOf(PaymentProviderNonRetryableException.class);
    }

    @Test
    void classifiesNestedTransportFailureAsRetryable() {
        RuntimeException result = StripePaymentClient.translateUnexpected(
                new IllegalStateException(
                        "Transport wrapper",
                        new SocketTimeoutException("Timed out")
                )
        );

        assertThat(result).isInstanceOf(PaymentProviderRetryableException.class);
    }

    @Test
    void classifiesUnexpectedAdapterFailureAsNonRetryable() {
        RuntimeException result = StripePaymentClient.translateUnexpected(
                new IllegalArgumentException("Invalid local request")
        );

        assertThat(result).isInstanceOf(PaymentProviderNonRetryableException.class);
    }
}
