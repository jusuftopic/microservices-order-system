package org.example.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.events.PaymentCompletedEvent;
import org.example.messagingstarter.contracts.events.PaymentFailedEvent;
import org.example.messagingstarter.contracts.commands.RefundPaymentCommand;
import org.example.messagingstarter.contracts.commands.ProcessPaymentCommand;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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
    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;
    private final OutboxDlqService outboxDlqService;
    private final PaymentMetrics paymentMetrics;

    /**
     * Creates a payment for a given order.
     *
     * @param event Payment request
     */
    @Transactional
    public void processPayment(ProcessPaymentCommand event) {
        int inserted = inboxRepository.insertIfNotExists(event.messageId());

        if (inserted == 0) {
            log.warn("[PAYMENT-SERVICE] Order {} already processed.", event.orderId());
            return;
        }

        incrementMetrics(paymentMetrics.getPaymentRequestsTotal());

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

        /* process payment progress */
        payment.setStatus(PaymentStatus.PROCESSING);
        final Payment saved = repository.save(payment);

        log.info("[PAYMENT-SERVICE] Payment {} for order {} set in processing state.", event.orderId(), saved.getId());

        /* publish event to initiate request to 3rd party payment provider */
        publishPaymentEvent(payment, event);
    }

    /**
     * IMPORTANT: do NOT call external systems here.
     * Instead, we publish an event that will be handled AFTER COMMIT.
     *
     * Reason:
     *  - avoid mixing transactional persistence and external 3rd party calls
     *  - DB lock should not be active during the network call
     *  - retry/rollback can get messy in slow 3rd party call
     */
    private void publishPaymentEvent(final Payment payment, final ProcessPaymentCommand event) {
        eventPublisher.publishEvent(
                new PaymentProcessingEvent(
                        payment.getId(),
                        event.orderId(),
                        event.correlationId()
                )
        );
    }

    private Payment createPayment(ProcessPaymentCommand event) {
        return Payment.builder()
                .orderId(event.orderId())
                .status(PaymentStatus.PENDING)
                .correlationId(event.correlationId())
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
            log.info("[PAYMENT-SERVICE] Payment {} processed successfully. Provider {}", paymentId, result.provider());

            incrementMetrics(paymentMetrics.getPaymentCompletedTotal());

            storeOutbox(
                    new PaymentCompletedEvent(
                            payment.getOrderId(),
                            payment.getCorrelationId(),
                            UUID.randomUUID()
                    ),
                    EventConstants.EVENT_PAYMENT_SUCCESS,
                    payment.getOrderId()
            );

        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(result.failureReason());
            log.warn("[PAYMENT-SERVICE] Payment {} processed failed. Provider {}. Reason: {}",
                    paymentId, result.provider(), result.failureReason());

            incrementMetrics(paymentMetrics.getPaymentFailedTotal());

            storeOutbox(
                    new PaymentFailedEvent(
                            payment.getOrderId(),
                            result.failureReason(),
                            payment.getCorrelationId(),
                            UUID.randomUUID()
                    ),
                    EventConstants.EVENT_PAYMENT_FAILED,
                    payment.getOrderId()
            );

        }
        repository.save(payment);
    }

    private void storeOutbox(Object payload,
                             String eventType,
                             Long aggregateId) {

        try {

            OutboxEvent event = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateType("PAYMENT")
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .processed(Boolean.FALSE)
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            outboxRepository.save(event);

            log.info("[PAYMENT-SERVICE] Stored outbox event {} ({})",
                    event.getId(), eventType);

        } catch (Exception e) {

            log.error("[PAYMENT-SERVICE] Failed to serialize payload for event {}",
                    eventType, e);

            outboxDlqService.storeOutboxDlq(
                    null,
                    aggregateId,
                    eventType,
                    payload,
                    0,
                    e
            );
        }
    }

    /**
     * Handles payment refund.
     *
     * @param event {@link RefundPaymentCommand} to handle
     */
    public void handleRefund(RefundPaymentCommand event) {
        incrementMetrics(paymentMetrics.getPaymentRefundRequestsTotal());
        incrementMetrics(paymentMetrics.getPaymentRefundCompletedTotal());
    }

    private void incrementMetrics(final Counter counter) {
        if (counter != null) counter.increment();
    }
}
