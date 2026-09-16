package org.example.paymentservice.service.provider.webhook;

import com.stripe.model.PaymentIntent;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.service.PaymentService;
import org.example.paymentservice.service.provider.StripePaymentResultMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StripeWebhookHandlerTest {

    private static final UUID IDEMPOTENCY_KEY =
            UUID.fromString("784b3660-cd8f-4f4e-bf12-55d7cc7e43bb");

    @Mock
    private PaymentService paymentService;

    private StripeWebhookHandler handler;

    @BeforeEach
    void setUp() {
        handler = new StripeWebhookHandler(
                paymentService,
                new StripePaymentResultMapper()
        );
    }

    @Test
    void appliesFirstDeliveryOfSupportedPaymentIntentEvent() {
        PaymentIntent intent = paymentIntent("pi_success", "succeeded", 42L);
        StripeWebhookEvent event = new StripeWebhookEvent(
                "evt_success", "payment_intent.succeeded", intent
        );

        boolean handled = handler.handle(event);

        assertThat(handled).isTrue();
        ArgumentCaptor<PaymentResultDTO> result =
                ArgumentCaptor.forClass(PaymentResultDTO.class);
        verify(paymentService).finalizePayment(
                org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.eq(IDEMPOTENCY_KEY),
                result.capture()
        );
        assertThat(result.getValue().transactionId()).isEqualTo("pi_success");
        assertThat(result.getValue().success()).isTrue();
    }

    @Test
    void ignoresUnrelatedEventTypeWithoutDatabaseMutation() {
        boolean handled = handler.handle(new StripeWebhookEvent(
                "evt_unhandled", "customer.created", null
        ));

        assertThat(handled).isFalse();
        verifyNoInteractions(paymentService);
    }

    private PaymentIntent paymentIntent(String id, String status, Long paymentId) {
        PaymentIntent intent = new PaymentIntent();
        intent.setId(id);
        intent.setStatus(status);
        intent.setMetadata(Map.of(
                "payment_id", paymentId.toString(),
                "idempotency_key", IDEMPOTENCY_KEY.toString()
        ));
        return intent;
    }
}
