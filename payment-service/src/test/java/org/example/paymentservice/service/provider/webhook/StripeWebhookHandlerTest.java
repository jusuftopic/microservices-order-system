package org.example.paymentservice.service.provider.webhook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StripeWebhookHandlerTest {

    private final StripeWebhookHandler handler = new StripeWebhookHandler();

    @Test
    void acceptsRequiredPaymentIntentEventType() {
        boolean handled = handler.handle(new StripeWebhookEvent(
                "evt_supported", "payment_intent.succeeded", "{}"
        ));

        assertThat(handled).isTrue();
    }

    @Test
    void ignoresUnrelatedEventType() {
        boolean handled = handler.handle(new StripeWebhookEvent(
                "evt_unhandled", "customer.created", "{}"
        ));

        assertThat(handled).isFalse();
    }
}
