package org.example.paymentservice.service.provider.webhook;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.example.paymentservice.exception.InvalidPaymentWebhookException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Authenticates incoming Stripe webhook payloads before they enter payment
 * processing.
 */
@Component
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
public class StripeWebhookVerifier {

    private final String webhookSecret;

    public StripeWebhookVerifier(
            @Value("${app.payment.stripe.webhook-secret}") String webhookSecret
    ) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException(
                    "Stripe webhook secret must be configured when Stripe is enabled"
            );
        }
        this.webhookSecret = webhookSecret;
    }

    /**
     * Verifies the signature against the unchanged HTTP request body.
     *
     * @param rawPayload unchanged request body
     * @param signature Stripe-Signature header value
     * @return authenticated event data
     * @throws InvalidPaymentWebhookException when authentication fails
     */
    public StripeWebhookEvent verify(String rawPayload, String signature) {
        if (rawPayload == null || rawPayload.isBlank()) {
            throw new InvalidPaymentWebhookException("Stripe webhook payload is missing");
        }
        if (signature == null || signature.isBlank()) {
            throw new InvalidPaymentWebhookException("Stripe-Signature header is missing");
        }

        try {
            Event event = Webhook.constructEvent(rawPayload, signature, webhookSecret);
            return new StripeWebhookEvent(event.getId(), event.getType(), rawPayload);
        } catch (SignatureVerificationException exception) {
            throw new InvalidPaymentWebhookException(
                    "Stripe webhook signature verification failed",
                    exception
            );
        } catch (RuntimeException exception) {
            throw new InvalidPaymentWebhookException(
                    "Stripe webhook payload is invalid",
                    exception
            );
        }
    }
}
