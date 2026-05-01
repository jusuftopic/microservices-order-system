package org.example.paymentservice.service;

import org.example.commons.event.PaymentRequestedEvent;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.PaymentStatus;
import org.example.paymentservice.repository.InboxRepository;
import org.example.paymentservice.repository.PaymentRepository;
import org.example.paymentservice.service.provider.PaymentProviderWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

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
    private PaymentProviderWrapper paymentProvider;

    /* class under test */
    private PaymentService target;

    @BeforeEach
    public void setUp() {
        target = new PaymentService(
                repository, inboxRepository, paymentProvider
        );
    }

    @Test
    void should_process_payment_successfully() {

        // GIVEN
        UUID eventId = UUID.randomUUID();

        PaymentRequestedEvent event = new PaymentRequestedEvent(
                eventId, 1L, BigDecimal.valueOf(1L), "test-email");

        when(inboxRepository.insertIfNotExists(eventId)).thenReturn(1);

        when(repository.findByOrderId(1L)).thenReturn(null);

        when(paymentProvider.pay(1L, eventId))
                .thenReturn(new PaymentResultDTO(true, "tx-123", null));

        // WHEN
        target.processPayment(event);

        // THEN
        verify(inboxRepository).insertIfNotExists(eventId);
        verify(paymentProvider).pay(1L, eventId);
        verify(repository, atLeastOnce()).save(any(Payment.class));
    }

    @Test
    void should_skip_processing_when_event_already_exists_in_inbox() {

        // GIVEN
        UUID eventId = UUID.randomUUID();
        PaymentRequestedEvent event = new PaymentRequestedEvent(
                eventId, 1L, BigDecimal.valueOf(1L), "test-email");

        when(inboxRepository.insertIfNotExists(eventId)).thenReturn(0);

        // WHEN
        target.processPayment(event);

        // THEN
        verify(inboxRepository).insertIfNotExists(eventId);
        verifyNoInteractions(repository);
        verifyNoInteractions(paymentProvider);
    }

    @Test
    void should_skip_when_payment_already_success() {

        // GIVEN
        UUID eventId = UUID.randomUUID();
        PaymentRequestedEvent event = new PaymentRequestedEvent(
                eventId, 1L, BigDecimal.valueOf(1L), "test-email");

        when(inboxRepository.insertIfNotExists(eventId)).thenReturn(1);

        Payment existingPayment = Payment.builder()
                .orderId(1L)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(repository.findByOrderId(1L)).thenReturn(existingPayment);

        // WHEN
        target.processPayment(event);

        // THEN
        verify(paymentProvider, never()).pay(anyLong(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void should_mark_payment_failed_when_provider_returns_failure() {

        // GIVEN
        UUID eventId = UUID.randomUUID();
        PaymentRequestedEvent event = new PaymentRequestedEvent(
                eventId, 1L, BigDecimal.valueOf(1L), "test-email");

        when(inboxRepository.insertIfNotExists(eventId)).thenReturn(1);
        when(repository.findByOrderId(1L)).thenReturn(null);

        when(paymentProvider.pay(1L, eventId))
                .thenReturn(new PaymentResultDTO(false, null, "INSUFFICIENT_FUNDS"));

        // WHEN
        target.processPayment(event);

        // THEN
        verify(paymentProvider).pay(1L, eventId);
        verify(repository, atLeastOnce()).save(any(Payment.class));
    }


}
