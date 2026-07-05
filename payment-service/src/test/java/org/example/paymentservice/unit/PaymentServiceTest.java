package org.example.paymentservice.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.PaymentRequestedEvent;
import org.example.messagingstarter.inbox.repository.InboxRepository;
import org.example.messagingstarter.outbox.entity.OutboxEvent;
import org.example.messagingstarter.outbox.repository.OutboxRepository;
import org.example.messagingstarter.outbox.service.OutboxDlqService;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.PaymentStatus;
import org.example.paymentservice.event.PaymentProcessingEvent;
import org.example.paymentservice.metrics.PaymentMetrics;
import org.example.paymentservice.repository.PaymentRepository;
import org.example.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
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

    /* class under test */
    private PaymentService target;

    @BeforeEach
    public void setUp() {
        target = new PaymentService(
                repository, inboxRepository, eventPublisher,
                new ObjectMapper(), outboxRepository, outboxDlqService,
                paymentMetrics
        );
    }

    @Test
    void should_process_payment_successfully() {

        // GIVEN
        final String correlationId = "11x11";
        final UUID messageId = UUID.randomUUID();
        PaymentRequestedEvent event = new PaymentRequestedEvent(1L, BigDecimal.ONE, "test", correlationId, messageId);

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);

        when(repository.findByOrderId(1L)).thenReturn(null);


        // WHEN
        target.processPayment(event);

        // THEN
        verify(inboxRepository).insertIfNotExists(messageId);
        verify(eventPublisher).publishEvent(any(PaymentProcessingEvent.class));
        verify(repository, atLeastOnce()).save(any(Payment.class));
    }

    @Test
    void should_skip_processing_when_event_already_exists_in_inbox() {

        // GIVEN
        final String correlationId = "11x11";
        final UUID messageId = UUID.randomUUID();
        PaymentRequestedEvent event = new PaymentRequestedEvent(1L, BigDecimal.ONE, "test", correlationId, messageId);

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
        PaymentRequestedEvent event = new PaymentRequestedEvent(1L, BigDecimal.ONE, "test", correlationId, messageId);

        when(inboxRepository.insertIfNotExists(messageId)).thenReturn(1);

        Payment existingPayment = Payment.builder()
                .orderId(1L)
                .status(PaymentStatus.SUCCESS)
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

        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.PROCESSING)
                .build();

        PaymentResultDTO result = new PaymentResultDTO(
                true,
                "tx-123",
                null,
                "test"
        );

        when(repository.findById(paymentId)).thenReturn(Optional.of(payment));

        // WHEN
        target.finalizePayment(paymentId, result);

        // THEN
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("tx-123", payment.getTransactionId());

        verify(repository).findById(paymentId);
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

        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.PROCESSING)
                .build();

        PaymentResultDTO result = new PaymentResultDTO(
                false,
                null,
                "card_declined",
                "test"
        );

        when(repository.findById(paymentId)).thenReturn(Optional.of(payment));

        // WHEN
        target.finalizePayment(paymentId, result);

        // THEN
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("card_declined", payment.getFailureReason());

        verify(repository).findById(paymentId);
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
    void should_throw_exception_when_payment_not_found() {
        // GIVEN
        Long paymentId = 1L;

        PaymentResultDTO result = new PaymentResultDTO(
                true,
                "tx-123",
                null,
                "test"
        );

        when(repository.findById(paymentId)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(NoSuchElementException.class,
                () -> target.finalizePayment(paymentId, result));

        verify(repository).findById(paymentId);
        verify(repository, never()).save(any());
    }

}
