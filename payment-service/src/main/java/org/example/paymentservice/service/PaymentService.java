package org.example.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.PaymentRequestedEvent;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.entity.InboxEvent;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.PaymentStatus;
import org.example.paymentservice.repository.InboxRepository;
import org.example.paymentservice.repository.PaymentRepository;
import org.example.paymentservice.service.provider.PaymentProviderWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Business logic for handling payments.
 *
 * Current behavior:
 * - Creates payment for an order
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository repository;
    private final InboxRepository inboxRepository;
    private final PaymentProviderWrapper paymentProvider;

    /**
     * Creates a payment for a given order.
     *
     * @param event Payment request
     */
    @Transactional
    public void processPayment(PaymentRequestedEvent event) {
        int inserted = inboxRepository.insertIfNotExists(event.eventId());

        if (inserted == 0) {
            log.info("[PAYMENT-SERVICE] Order {} already processed.", event.orderId());
            return;
        }

        processPaymentInternal(event);
    }

    private void processPaymentInternal(PaymentRequestedEvent event) {
        Payment payment = Optional.ofNullable(repository.findByOrderId(event.orderId()))
                .orElseGet(() -> createPayment(event));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("[PAYMENT-SERVICE] Order {} already successfully stored.", event.orderId());
            return;
        }

        if (payment.getStatus() == PaymentStatus.PROCESSING) {
            log.info("[PAYMENT-SERVICE] Order {} still processing.", event.orderId());
            return;
        }

        payment.setStatus(PaymentStatus.PROCESSING);
        repository.save(payment);

        try {
            final PaymentResultDTO paymentResult = paymentProvider.pay(event.orderId(), event.eventId());

            if (paymentResult.success()) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setTransactionId(paymentResult.transactionId());
            }
            else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(paymentResult.failureReason());
            }
        }
        catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("TECHNICAL_ERROR");
            log.error("[PAYMENT-SERVICE] Failed to process payment for event {} and order {}",
                    event.eventId(), event.orderId(), e);
            throw e;
        }

        final InboxEvent inbox = new InboxEvent();
        inbox.setEventId(event.eventId());

        inboxRepository.save(inbox);
        repository.save(payment);
    }

    private Payment createPayment(PaymentRequestedEvent event) {
        return Payment.builder()
                .orderId(event.orderId())
                .status(PaymentStatus.PENDING)
                .build();
    }
}
