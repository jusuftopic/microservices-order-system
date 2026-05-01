package org.example.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.PaymentRequestedEvent;
import org.example.paymentservice.dto.PaymentResultDTO;
import org.example.paymentservice.entity.InboxEvent;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.mapper.PaymentMapper;
import org.example.paymentservice.repository.InboxRepository;
import org.example.paymentservice.repository.PaymentRepository;
import org.example.paymentservice.service.provider.PaymentProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    private final PaymentProvider paymentProvider;

    /**
     * Creates a payment for a given order.
     *
     * @param event Payment request
     */
    @Transactional
    public void processPayment(PaymentRequestedEvent event) {
        if (inboxRepository.existsById(event.eventId())) {
            log.info("[PAYMENT-SERVICE] Order {} already processed.", event.orderId());
            return;
        }

        storeInboxEntity(event);
        final PaymentResultDTO paymentResult = paymentProvider.pay(event.orderId());
        storePayment(event, paymentResult);
    }


    private void storeInboxEntity(PaymentRequestedEvent event) {
        final InboxEvent inbox = new InboxEvent();
        inbox.setEventId(event.eventId());
        inboxRepository.save(inbox);
    }

    private void storePayment(PaymentRequestedEvent event, final PaymentResultDTO paymentResult) {
        Payment entity;
        if (paymentResult == null) {
            log.warn("[PAYMENT-SERVICE] Payment result from provider is NULL for the event {} and order {}",
                    event.eventId(), event.orderId());
            entity = PaymentMapper.toEntity(event.orderId(), null, "MOCK");
        }
        else {
            entity = PaymentMapper.toEntity(event.orderId(), paymentResult, "MOCK");
        }

        repository.save(entity);
        log.info("[PAYMENT-SERVICE] Payment for the order {} is processed.", event.orderId());
    }
}
