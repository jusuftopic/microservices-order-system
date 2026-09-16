package org.example.paymentservice.controller;

import org.example.paymentservice.exception.InvalidPaymentWebhookException;
import org.example.paymentservice.service.provider.webhook.StripeWebhookEvent;
import org.example.paymentservice.service.provider.webhook.StripeWebhookHandler;
import org.example.paymentservice.service.provider.webhook.StripeWebhookVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentWebhookControllerTest {

    @Test
    void acknowledgesVerifiedWebhook() {
        StripeWebhookEvent event = new StripeWebhookEvent(
                "evt_1", "payment_intent.succeeded", null
        );
        StubWebhookVerifier verifier = new StubWebhookVerifier(event, null);
        RecordingWebhookHandler handler = new RecordingWebhookHandler();
        PaymentWebhookController controller = new PaymentWebhookController(verifier, handler);

        ResponseEntity<Void> response =
                controller.handleStripeWebhook("payload", "signature");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(handler.handledEvent).isEqualTo(event);
    }

    @Test
    void rejectsInvalidWebhookWithoutHandlingIt() {
        StubWebhookVerifier verifier = new StubWebhookVerifier(
                null,
                new InvalidPaymentWebhookException("missing signature")
        );
        RecordingWebhookHandler handler = new RecordingWebhookHandler();
        PaymentWebhookController controller = new PaymentWebhookController(verifier, handler);

        ResponseEntity<Void> response = controller.handleStripeWebhook("payload", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(handler.handledEvent).isNull();
    }

    private static final class StubWebhookVerifier extends StripeWebhookVerifier {

        private final StripeWebhookEvent event;
        private final InvalidPaymentWebhookException failure;

        private StubWebhookVerifier(
                StripeWebhookEvent event,
                InvalidPaymentWebhookException failure
        ) {
            super("whsec_test");
            this.event = event;
            this.failure = failure;
        }

        @Override
        public StripeWebhookEvent verify(String rawPayload, String signature) {
            if (failure != null) {
                throw failure;
            }
            return event;
        }
    }

    private static final class RecordingWebhookHandler extends StripeWebhookHandler {

        private RecordingWebhookHandler() {
            super(null, null);
        }

        private StripeWebhookEvent handledEvent;

        @Override
        public boolean handle(StripeWebhookEvent event) {
            handledEvent = event;
            return true;
        }
    }
}
