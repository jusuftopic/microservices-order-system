package org.example.paymentservice.unit.service;

import org.example.commons.event.PaymentRequestedEvent;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.PaymentStatus;
import org.example.paymentservice.event.PaymentProcessingEvent;
import org.example.paymentservice.repository.InboxRepository;
import org.example.paymentservice.repository.PaymentRepository;
import org.example.paymentservice.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
    private ApplicationEventPublisher eventPublisher;

    /* class under test */
    private PaymentService target;

    @BeforeEach
    public void setUp() {
        target = new PaymentService(
                repository, inboxRepository, eventPublisher
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


        // WHEN
        target.processPayment(event);

        // THEN
        verify(inboxRepository).insertIfNotExists(eventId);
        verify(eventPublisher).publishEvent(any(PaymentProcessingEvent.class));
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
        verifyNoInteractions(eventPublisher);
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
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(PaymentProcessingEvent.class));

    }
}
