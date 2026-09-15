package org.example.paymentservice.service.provider.webhook;

import org.example.paymentservice.exception.InvalidPaymentWebhookException;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripeWebhookVerifierTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret";
    private static final String PAYLOAD = """
            {
              "id": "evt_payment_succeeded",
              "object": "event",
              "type": "payment_intent.succeeded",
              "data": {"object": {"id": "pi_test", "object": "payment_intent"}}
            }
            """;

    private final StripeWebhookVerifier verifier =
            new StripeWebhookVerifier(WEBHOOK_SECRET);

    @Test
    void acceptsValidSignatureForRawPayload() throws Exception {
        String signature = signatureFor(PAYLOAD);

        StripeWebhookEvent result = verifier.verify(PAYLOAD, signature);

        assertThat(result.id()).isEqualTo("evt_payment_succeeded");
        assertThat(result.type()).isEqualTo("payment_intent.succeeded");
        assertThat(result.rawPayload()).isEqualTo(PAYLOAD);
    }

    @Test
    void rejectsSignatureCreatedForDifferentPayload() throws Exception {
        String signature = signatureFor(PAYLOAD);

        assertThatThrownBy(() -> verifier.verify(PAYLOAD + " ", signature))
                .isInstanceOf(InvalidPaymentWebhookException.class);
    }

    @Test
    void rejectsMissingSignature() {
        assertThatThrownBy(() -> verifier.verify(PAYLOAD, null))
                .isInstanceOf(InvalidPaymentWebhookException.class)
                .hasMessageContaining("Stripe-Signature");
    }

    private String signatureFor(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));
        byte[] digest = mac.doFinal(
                (timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)
        );
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(digest);
    }
}
