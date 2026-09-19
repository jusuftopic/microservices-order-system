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
import org.example.paymentservice.dto.RefundRequest;
import org.example.paymentservice.dto.RefundResult;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.RefundStatus;
import org.example.paymentservice.enums.PaymentStatus;
import org.example.paymentservice.enums.PaymentProviderStatus;
import org.example.paymentservice.event.PaymentProcessingEvent;
import org.example.paymentservice.metrics.PaymentMetrics;
import org.example.paymentservice.repository.PaymentRepository;
import org.example.paymentservice.service.provider.PaymentProviderWrapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
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

    public static final String INTERACTIVE_ACTION_UNSUPPORTED =
            "INTERACTIVE_PAYMENT_ACTION_UNSUPPORTED";

    private final PaymentRepository repository;
    private final InboxRepository inboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final OutboxRepository outboxRepository;
    private final OutboxDlqService outboxDlqService;
    private final PaymentMetrics paymentMetrics;
    private final PaymentStatusTransitionPolicy transitionPolicy;
    private final PaymentProviderWrapper paymentProviderWrapper;

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

        if (payment.getStatus().isFinalState()) {
            log.info("[PAYMENT-SERVICE] Payment for order {} is already in final state {}.",
                    event.orderId(), payment.getStatus());
            return;
        }

        if (!Objects.equals(payment.getProviderIdempotencyKey(), event.messageId())) {
            throw new IllegalStateException(
                    "Order already belongs to a different payment operation"
            );
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
                        event.amount(),
                        event.correlationId(),
                        event.messageId()
                )
        );
    }

    private Payment createPayment(ProcessPaymentCommand event) {
        return Payment.builder()
                .orderId(event.orderId())
                .status(PaymentStatus.PENDING)
                .refundStatus(RefundStatus.NOT_REQUESTED)
                .correlationId(event.correlationId())
                .providerIdempotencyKey(event.messageId())
                .build();
    }

    /**
     * Initiates a refund for the successful payment associated with an order.
     * Repeated calls reuse the same provider idempotency key.
     *
     * @param refundCommand Refund payment command
     */
    public void refundPayment(RefundPaymentCommand refundCommand) {
        int inserted = inboxRepository.insertIfNotExists(refundCommand.messageId());

        if (inserted == 0) {
            log.warn("[PAYMENT-SERVICE] Order {} already processed.",refundCommand.orderId());
            return;
        }

        final long orderId = refundCommand.orderId();

        incrementMetrics(paymentMetrics.getPaymentRefundRequestsTotal());
        Payment payment = Optional.ofNullable(repository.findByOrderId(orderId))
                .orElseThrow(() -> new IllegalStateException(
                        "No payment exists for refund order " + orderId
                ));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Only a successful payment can be refunded");
        }
        if (payment.getTransactionId() == null || payment.getTransactionId().isBlank()) {
            throw new IllegalStateException(
                    "Successful payment has no provider transaction identifier"
            );
        }

        RefundStatus currentRefundStatus = Optional.ofNullable(payment.getRefundStatus())
                .orElse(RefundStatus.NOT_REQUESTED);
        if (currentRefundStatus.isFinalState()) {
            log.info(
                    "[PAYMENT-SERVICE][REFUND] Payment {} already has final refund state {}",
                    payment.getId(),
                    currentRefundStatus
            );
            return;
        }

        RefundRequest request = new RefundRequest(
                payment.getId(),
                payment.getOrderId(),
                payment.getTransactionId(),
                null,
                "refund-" + payment.getProviderIdempotencyKey()
        );

        payment.setRefundStatus(RefundStatus.PROCESSING);
        repository.save(payment);

        RefundResult result = paymentProviderWrapper.refund(request);
        payment.setRefundStatus(
                result.succeeded() ? RefundStatus.SUCCESS : RefundStatus.FAILED
        );
        repository.save(payment);

        log.info(
                "[PAYMENT-SERVICE][REFUND] Payment {} refund completed with state {}",
                payment.getId(),
                payment.getRefundStatus()
        );

        incrementMetrics(paymentMetrics.getPaymentRefundCompletedTotal());
    }

    /**
     * Independent transaction after receiving a payment result:
     * - updates result from payment provider
     * - does NOT depend on original transaction
     * - applies a provider operation result at most once
     *
     * @param paymentId expected internal payment identifier
     * @param providerIdempotencyKey identifier of the provider operation
     * @param result provider result to apply
     */
    @Transactional
    public void finalizePayment(
            Long paymentId,
            UUID providerIdempotencyKey,
            PaymentResultDTO result
    ) {
        Payment payment = repository.findByProviderIdempotencyKey(providerIdempotencyKey)
                .orElseThrow(() -> new IllegalStateException(
                        "No payment exists for the supplied provider operation"
                ));

        if (!payment.getId().equals(paymentId)) {
            throw new IllegalStateException(
                    "Provider operation does not belong to the supplied payment"
            );
        }

        applyProviderResult(payment, result);
    }

    private void applyProviderResult(Payment payment, PaymentResultDTO result) {
        PaymentStatus currentStatus = payment.getStatus();
        PaymentStatus targetStatus = transitionPolicy.targetStatus(
                currentStatus,
                result.status()
        );

        if (currentStatus.isFinalState()) {
            log.info(
                    "[PAYMENT-SERVICE] Payment {} already has final state {}; "
                            + "ignoring provider state {}",
                    payment.getId(),
                    currentStatus,
                    result.status()
            );
            return;
        }

        payment.setProvider(result.provider());
        payment.setTransactionId(result.transactionId());
        payment.setStatus(targetStatus);

        if (targetStatus == PaymentStatus.SUCCESS) {
            log.info("[PAYMENT-SERVICE] Payment {} processed successfully. Provider {}",
                    payment.getId(), result.provider());

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

        } else if (targetStatus == PaymentStatus.FAILED) {
            String failureReason = result.status() == PaymentProviderStatus.REQUIRES_ACTION
                    ? INTERACTIVE_ACTION_UNSUPPORTED
                    : result.failureReason();

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(failureReason);

            if (result.status() == PaymentProviderStatus.REQUIRES_ACTION) {
                log.warn(
                        "[PAYMENT-SERVICE] Payment {} requires unsupported interactive action {} "
                                + "from provider {}; marking payment as failed",
                        payment.getId(),
                        result.nextActionType(),
                        result.provider()
                );
            } else {
                log.warn("[PAYMENT-SERVICE] Payment {} processed failed. Provider {}. Reason: {}",
                        payment.getId(), result.provider(), failureReason);
            }

            incrementMetrics(paymentMetrics.getPaymentFailedTotal());

            storeOutbox(
                    new PaymentFailedEvent(
                            payment.getOrderId(),
                            failureReason,
                            payment.getCorrelationId(),
                            UUID.randomUUID()
                    ),
                    EventConstants.EVENT_PAYMENT_FAILED,
                    payment.getOrderId()
            );

        } else {
            log.info(
                    "[PAYMENT-SERVICE] Payment {} remains in provider state {}. Provider {}",
                    payment.getId(),
                    result.status(),
                    result.provider()
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

    }

    private void incrementMetrics(final Counter counter) {
        if (counter != null) counter.increment();
    }
}
