package org.example.paymentservice.config;

import com.stripe.StripeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configures access to the isolated Stripe sandbox.
 */
@Configuration
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
public class StripeClientConfig {

    /**
     * Creates a sandbox client with bounded network timeouts. Retries remain in
     * the application resilience boundary so that one layer owns retry policy.
     */
    @Bean
    public StripeClient stripeClient(
            @Value("${app.payment.stripe.api-key}") String apiKey,
            @Value("${app.payment.stripe.connect-timeout}") Duration connectTimeout,
            @Value("${app.payment.stripe.read-timeout}") Duration readTimeout
    ) {
        if (!apiKey.startsWith("sk_test_")) {
            throw new IllegalStateException(
                    "Stripe sandbox integration requires an sk_test_ API key"
            );
        }

        return StripeClient.builder()
                .setApiKey(apiKey)
                .setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()))
                .setReadTimeout(Math.toIntExact(readTimeout.toMillis()))
                .setMaxNetworkRetries(0)
                .build();
    }
}
