package org.example.paymentservice.integration;

import com.stripe.model.PaymentIntent;
import org.example.messagingstarter.outbox.repository.OutboxRepository;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.PaymentStatus;
import org.example.paymentservice.repository.PaymentRepository;
import org.example.paymentservice.service.PaymentService;
import org.example.paymentservice.service.provider.StripePaymentResultMapper;
import org.example.paymentservice.service.provider.webhook.StripeWebhookEvent;
import org.example.paymentservice.service.provider.webhook.StripeWebhookHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StripeWebhookProcessingIT extends AbstractIntegrationTest {

    private static final UUID IDEMPOTENCY_KEY =
            UUID.fromString("784b3660-cd8f-4f4e-bf12-55d7cc7e43bb");

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private StripeWebhookHandler handler;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        paymentRepository.deleteAll();
        handler = new StripeWebhookHandler(
                paymentService,
                new StripePaymentResultMapper()
        );
    }

    @Test
    void duplicateDeliveryProducesOneTransitionAndOneOutboxEvent() {
        Payment payment = saveProcessingPayment();
        StripeWebhookEvent event = event("evt_success", "payment_intent.succeeded",
                "succeeded", payment.getId());

        boolean firstDelivery = inTransaction(() -> handler.handle(event));
        boolean duplicateDelivery = inTransaction(() -> handler.handle(event));

        assertThat(firstDelivery).isTrue();
        assertThat(duplicateDelivery).isTrue();
        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);
        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    @Test
    void processingObservationCanBeFollowedByTerminalWebhookResult() {
        Payment payment = saveProcessingPayment();
        StripeWebhookEvent processing = event("evt_processing",
                "payment_intent.processing", "processing", payment.getId());
        StripeWebhookEvent success = event("evt_success",
                "payment_intent.succeeded", "succeeded", payment.getId());

        inTransaction(() -> handler.handle(processing));
        inTransaction(() -> handler.handle(success));

        Payment stored = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(stored.getTransactionId()).isEqualTo("pi_webhook_it");
        assertThat(outboxRepository.count()).isEqualTo(1);
    }

    private Payment saveProcessingPayment() {
        return paymentRepository.save(Payment.builder()
                .orderId(9001L)
                .status(PaymentStatus.PROCESSING)
                .correlationId("stripe-webhook-it")
                .providerIdempotencyKey(IDEMPOTENCY_KEY)
                .build());
    }

    private StripeWebhookEvent event(
            String eventId,
            String eventType,
            String intentStatus,
            Long paymentId
    ) {
        PaymentIntent intent = new PaymentIntent();
        intent.setId("pi_webhook_it");
        intent.setStatus(intentStatus);
        intent.setMetadata(Map.of(
                "payment_id", paymentId.toString(),
                "idempotency_key", IDEMPOTENCY_KEY.toString()
        ));
        return new StripeWebhookEvent(eventId, eventType, intent);
    }

    private boolean inTransaction(java.util.function.Supplier<Boolean> action) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> action.get()));
    }
}
