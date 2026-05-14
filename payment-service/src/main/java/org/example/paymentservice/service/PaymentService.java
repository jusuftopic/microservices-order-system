package org.example.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.PaymentRequestedEvent;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.PaymentStatus;
import org.example.paymentservice.event.PaymentProcessingEvent;
import org.example.paymentservice.repository.InboxRepository;
import org.example.paymentservice.repository.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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

        /**
         * IMPORTANT: do NOT call external systems here.
         * Instead, we publish an event that will be handled AFTER COMMIT.
         *
         * Reason:
         *  - avoid mixing transactional persistence and external 3rd party calls
         *  - DB lock should not be active during the network call
         *  - retry/rollback can get messy in slow 3rd party call
         */
        eventPublisher.publishEvent(
                new PaymentProcessingEvent(
                        payment.getId(),
                        event.orderId(),
                        event.eventId()
                )
        );
    }

    private Payment createPayment(PaymentRequestedEvent event) {
        return Payment.builder()
                .orderId(event.orderId())
                .status(PaymentStatus.PENDING)
                .build();
    }

    /**
     * Independent transaction after receiving payment result:
     * - updates result from payment provider
     * - does NOT depend on original transaction
     */
    @Transactional
    public void finalizePayment(Long paymentId, PaymentResultDTO result) {
        Payment payment = repository.findById(paymentId)
                .orElseThrow();

        payment.setProvider(result.provider());

        if (result.success()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(result.transactionId());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(result.failureReason());
        }

        repository.save(payment);
    }
}
