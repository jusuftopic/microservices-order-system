package org.example.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.exception.InvalidPaymentWebhookException;
import org.example.paymentservice.service.provider.webhook.StripeWebhookEvent;
import org.example.paymentservice.service.provider.webhook.StripeWebhookHandler;
import org.example.paymentservice.service.provider.webhook.StripeWebhookVerifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP boundary for payment-provider callbacks.
 */
@RestController
@RequestMapping("/api/payments")
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "stripe")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final StripeWebhookVerifier webhookVerifier;
    private final StripeWebhookHandler webhookHandler;

    /**
     * Authenticates and accepts Stripe webhook events.
     *
     * @param rawPayload unchanged HTTP request body
     * @param signature Stripe-Signature header
     * @return no-content acknowledgement, or bad request for invalid input
     */
    @PostMapping("/webhooks/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature
    ) {
        try {
            StripeWebhookEvent event = webhookVerifier.verify(rawPayload, signature);
            webhookHandler.handle(event);
            return ResponseEntity.noContent().build();
        } catch (InvalidPaymentWebhookException exception) {
            log.warn("[PAYMENT-SERVICE][STRIPE-WEBHOOK] Rejected invalid webhook: {}",
                    exception.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
