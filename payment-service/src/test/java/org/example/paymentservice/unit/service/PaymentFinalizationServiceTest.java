package org.example.paymentservice.unit.service;

import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.PaymentStatus;
import org.example.paymentservice.repository.PaymentRepository;
import org.example.paymentservice.service.PaymentFinalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests {@link org.example.paymentservice.service.PaymentFinalizationService}
 */
@ExtendWith(MockitoExtension.class)
public class PaymentFinalizationServiceTest {

    @Mock
    private PaymentRepository repository;

    /* class under test */
    private PaymentFinalizationService target;

    @BeforeEach
    void setUp() {
        target = new PaymentFinalizationService(repository);
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
                null
        );

        when(repository.findById(paymentId)).thenReturn(Optional.of(payment));

        // WHEN
        target.finalizePayment(paymentId, result);

        // THEN
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("tx-123", payment.getTransactionId());

        verify(repository).findById(paymentId);
        verify(repository).save(payment);

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
                "card_declined"
        );

        when(repository.findById(paymentId)).thenReturn(Optional.of(payment));

        // WHEN
        target.finalizePayment(paymentId, result);

        // THEN
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("card_declined", payment.getFailureReason());

        verify(repository).findById(paymentId);
        verify(repository).save(payment);
    }

    @Test
    void should_throw_exception_when_payment_not_found() {

        // GIVEN
        Long paymentId = 1L;

        PaymentResultDTO result = new PaymentResultDTO(
                true,
                "tx-123",
                null
        );

        when(repository.findById(paymentId)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThrows(NoSuchElementException.class,
                () -> target.finalizePayment(paymentId, result));

        verify(repository).findById(paymentId);
        verify(repository, never()).save(any());

    }
}
