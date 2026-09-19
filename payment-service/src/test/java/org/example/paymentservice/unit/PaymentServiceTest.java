package org.example.paymentservice.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.commands.ProcessPaymentCommand;
import org.example.messagingstarter.contracts.commands.RefundPaymentCommand;
import org.example.messagingstarter.inbox.repository.InboxRepository;
import org.example.messagingstarter.outbox.entity.OutboxEvent;
import org.example.messagingstarter.outbox.repository.OutboxRepository;
import org.example.messagingstarter.outbox.service.OutboxDlqService;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.dto.RefundRequest;
import org.example.paymentservice.dto.RefundResult;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.RefundStatus;
import org.example.paymentservice.enums.PaymentStatus;
import org.example.paymentservice.enums.PaymentProviderStatus;
import org.example.paymentservice.event.PaymentProcessingEvent;
import org.example.paymentservice.metrics.PaymentMetrics;
import org.example.paymentservice.repository.PaymentRepository;
import org.example.paymentservice.service.PaymentService;
import org.example.paymentservice.service.PaymentStatusTransitionPolicy;
import org.example.paymentservice.service.provider.PaymentProviderWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests {@link PaymentService}
 */
@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository repository;

    @Mock
    private InboxRepository inboxRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxDlqService outboxDlqService;

    @Mock
    private PaymentMetrics paymentMetrics;

    @Mock
    private PaymentProviderWrapper paymentProviderWrapper;

    /* class under test */
    private PaymentService target;

    @BeforeEach
    public void setUp() {
        target = new PaymentService(
                repository, inboxRepository, eventPublisher,
                new ObjectMapper(), outboxRepository, outboxDlqService,
                paymentMetrics, new PaymentStatusTransitionPolicy(),
                paymentProviderWrapper
        );
    }

    @Test
    void should_refund_successful_payment() {
        final RefundPaymentCommand refundPaymentCommand = new RefundPaymentCommand(
                3L, "corrId1", UUID.randomUUID()
        );

        UUID providerIdempotencyKey = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(7L)
                .orderId(3L)
                .status(PaymentStatus.SUCCESS)
                .refundStatus(RefundStatus.NOT_REQUESTED)
                .transactionId("provider-payment-7")
                .providerIdempotencyKey(providerIdempotencyKey)
                .build();

        when(inboxRepository.insertIfNotExists(refundPaymentCommand.messageId())).thenReturn(1);
        when(repository.findByOrderId(refundPaymentCommand.orderId())).thenReturn(payment);
        when(paymentProviderWrapper.refund(any())).thenReturn(
                new RefundResult(true, "provider-refund-7", null)
        );

        target.refundPayment(refundPaymentCommand);

        ArgumentCaptor<RefundRequest> requestCaptor =
                ArgumentCaptor.forClass(RefundRequest.class);
        verify(paymentProviderWrapper).refund(requestCaptor.capture());
        assertEquals(7L, requestCaptor.getValue().refundOperationId());
        assertEquals(3L, requestCaptor.getValue().orderId());
        assertEquals("provider-payment-7", requestCaptor.getValue().providerPaymentId());
        assertEquals("refund-" + providerIdempotencyKey,
                requestCaptor.getValue().idempotencyKey());
        assertEquals(RefundStatus.SUCCESS, payment.getRefundStatus());
        verify(repository, times(2)).save(payment);
    }

    @Test
    void should_reject_refund_for_payment_that_is_not_successful() {
        final RefundPaymentCommand refundPaymentCommand = new RefundPaymentCommand(
                3L, "corrId1", UUID.randomUUID()
        );

        Payment payment = Payment.builder()
                .id(7L)
                .orderId(3L)
                .status(PaymentStatus.PROCESSING)
                .refundStatus(RefundStatus.NOT_REQUESTED)
                .build();

        when(inboxRepository.insertIfNotExists(refundPaymentCommand.messageId())).thenReturn(1);
        when(repository.findByOrderId(refundPaymentCommand.orderId())).thenReturn(payment);

        assertThrows(IllegalStateException.class, () -> target.refundPayment(refundPaymentCommand));

        verifyNoInteractions(paymentProviderWrapper);
        verify(repository, never()).save(any());
    }

    @Test
    void should_not_repeat_refund_in_final_state() {
        final RefundPaymentCommand refundPaymentCommand = new RefundPaymentCommand(
                3L, "corrId1", UUID.randomUUID()
        );

        Payment payment = Payment.builder()
                .id(7L)
                .orderId(3L)
                .status(PaymentStatus.SUCCESS)
                .refundStatus(RefundStatus.SUCCESS)
                .transactionId("provider-payment-7")
                .providerIdempotencyKey(UUID.randomUUID())
                .build();

        when(inboxRepository.insertIfNotExists(refundPaymentCommand.messageId())).thenReturn(1);
        when(repository.findByOrderId(refundPaymentCommand.orderId())).thenReturn(payment);

        target.refundPayment(refundPaymentCommand);

        verifyNoInteractions(paymentProviderWrapper);
        verify(repository, never()).save(any());
    }

    @Test
    void should_process_payment_successfully() {

        // GIVEN
        final String correlationId = "11x11";
        final UUID messageId = UUID.randomUUID();
        ProcessPaymentCommand event = new ProcessPaymentCommand(1L, BigDecimal.ONE, "test", correlationId, messageId);

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);

        when(repository.findByOrderId(1L)).thenReturn(null);

        final Payment saved = new Payment();
        saved.setId(1L);

        when(repository.save(any())).thenReturn(saved);

        // WHEN
        target.processPayment(event);

        // THEN
        verify(inboxRepository).insertIfNotExists(messageId);
        verify(eventPublisher).publishEvent(any(PaymentProcessingEvent.class));
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(paymentCaptor.capture());
        assertEquals(messageId, paymentCaptor.getValue().getProviderIdempotencyKey());
    }

    @Test
    void should_skip_processing_when_event_already_exists_in_inbox() {

        // GIVEN
        final String correlationId = "11x11";
        final UUID messageId = UUID.randomUUID();
        ProcessPaymentCommand event = new ProcessPaymentCommand(1L, BigDecimal.ONE, "test", correlationId, messageId);

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(0);

        // WHEN
        target.processPayment(event);

        // THEN
        verify(inboxRepository).insertIfNotExists(messageId);
        verifyNoInteractions(repository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void should_skip_when_payment_already_success() {

        // GIVEN
        final String correlationId = "11x11";
        final UUID messageId = UUID.randomUUID();
        ProcessPaymentCommand event = new ProcessPaymentCommand(1L, BigDecimal.ONE, "test", correlationId, messageId);

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);

        Payment existingPayment = Payment.builder()
                .orderId(1L)
                .status(PaymentStatus.SUCCESS)
                .providerIdempotencyKey(messageId)
                .build();

        when(repository.findByOrderId(1L)).thenReturn(existingPayment);

        // WHEN
        target.processPayment(event);

        // THEN
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(PaymentProcessingEvent.class));
    }

    @Test
    void should_finalize_payment_successfully() {

        // GIVEN
        Long paymentId = 1L;
        UUID idempotencyKey = UUID.randomUUID();

        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.PROCESSING)
                .providerIdempotencyKey(idempotencyKey)
                .build();

        PaymentResultDTO result = new PaymentResultDTO(
                PaymentProviderStatus.SUCCEEDED,
                "tx-123",
                null,
                null,
                "test"
        );

        when(repository.findByProviderIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(payment));

        // WHEN
        target.finalizePayment(paymentId, idempotencyKey, result);

        // THEN
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("tx-123", payment.getTransactionId());

        verify(repository).findByProviderIdempotencyKey(idempotencyKey);
        verify(repository).save(payment);


        // verify outbox event stored
        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());

        OutboxEvent storedEvent = captor.getValue();


        assertEquals("PAYMENT", storedEvent.getAggregateType());
        assertEquals(payment.getOrderId(), storedEvent.getAggregateId());
        assertEquals(EventConstants.EVENT_PAYMENT_SUCCESS, storedEvent.getEventType());
        assertFalse(storedEvent.getProcessed());
    }

    @Test
    void should_finalize_payment_as_failed_when_provider_returns_failure() {

        // GIVEN
        Long paymentId = 1L;
        UUID idempotencyKey = UUID.randomUUID();

        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.PROCESSING)
                .providerIdempotencyKey(idempotencyKey)
                .build();

        PaymentResultDTO result = new PaymentResultDTO(
                PaymentProviderStatus.FAILED,
                null,
                "card_declined",
                null,
                "test"
        );

        when(repository.findByProviderIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(payment));

        // WHEN
        target.finalizePayment(paymentId, idempotencyKey, result);

        // THEN
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("card_declined", payment.getFailureReason());

        verify(repository).findByProviderIdempotencyKey(idempotencyKey);
        verify(repository).save(payment);


        // verify outbox event stored
        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxRepository).save(captor.capture());

        OutboxEvent storedEvent = captor.getValue();

        assertEquals("PAYMENT", storedEvent.getAggregateType());
        assertEquals(payment.getOrderId(), storedEvent.getAggregateId());
        assertEquals(EventConstants.EVENT_PAYMENT_FAILED, storedEvent.getEventType());
        assertFalse(storedEvent.getProcessed());
    }

    @Test
    void should_fail_payment_when_provider_requires_interactive_action() {
        Long paymentId = 1L;
        UUID idempotencyKey = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(10L)
                .correlationId("correlation-10")
                .status(PaymentStatus.PROCESSING)
                .providerIdempotencyKey(idempotencyKey)
                .build();
        PaymentResultDTO result = new PaymentResultDTO(
                PaymentProviderStatus.REQUIRES_ACTION,
                "pi_requires_action",
                null,
                "use_stripe_sdk",
                "STRIPE"
        );
        when(repository.findByProviderIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(payment));

        target.finalizePayment(paymentId, idempotencyKey, result);

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(
                PaymentService.INTERACTIVE_ACTION_UNSUPPORTED,
                payment.getFailureReason()
        );
        verify(repository).save(payment);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        assertEquals(EventConstants.EVENT_PAYMENT_FAILED, captor.getValue().getEventType());
        assertTrue(captor.getValue().getPayload().contains(
                PaymentService.INTERACTIVE_ACTION_UNSUPPORTED
        ));
    }

    @Test
    void should_throw_exception_when_payment_not_found() {
        // GIVEN
        Long paymentId = 1L;
        UUID idempotencyKey = UUID.randomUUID();

        PaymentResultDTO result = new PaymentResultDTO(
                PaymentProviderStatus.SUCCEEDED,
                "tx-123",
                null,
                null,
                "test"
        );

        when(repository.findByProviderIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(IllegalStateException.class,
                () -> target.finalizePayment(paymentId, idempotencyKey, result));

        verify(repository).findByProviderIdempotencyKey(idempotencyKey);
        verify(repository, never()).save(any());
    }

    @Test
    void should_ignore_provider_result_when_payment_is_already_final() {
        Long paymentId = 1L;
        UUID idempotencyKey = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.SUCCESS)
                .providerIdempotencyKey(idempotencyKey)
                .build();
        PaymentResultDTO repeatedResult = new PaymentResultDTO(
                PaymentProviderStatus.SUCCEEDED,
                "tx-123",
                null,
                null,
                "test"
        );
        when(repository.findByProviderIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(payment));

        target.finalizePayment(paymentId, idempotencyKey, repeatedResult);

        verify(repository, never()).save(any());
        verifyNoInteractions(outboxRepository);
    }

    @Test
    void should_allow_processing_observation_before_terminal_webhook_result() {
        Long paymentId = 1L;
        UUID idempotencyKey = UUID.fromString(
                "784b3660-cd8f-4f4e-bf12-55d7cc7e43bb"
        );
        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(10L)
                .correlationId("correlation-10")
                .status(PaymentStatus.PROCESSING)
                .providerIdempotencyKey(idempotencyKey)
                .build();
        when(repository.findByProviderIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(payment));

        target.finalizePayment(paymentId, idempotencyKey, new PaymentResultDTO(
                PaymentProviderStatus.PROCESSING,
                "pi_success",
                null,
                null,
                "STRIPE"
        ));
        target.finalizePayment(paymentId, idempotencyKey, new PaymentResultDTO(
                PaymentProviderStatus.SUCCEEDED,
                "pi_success",
                null,
                null,
                "STRIPE"
        ));

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("pi_success", payment.getTransactionId());
        verify(repository, times(2)).save(payment);
        verify(outboxRepository).save(any(OutboxEvent.class));
    }

}
