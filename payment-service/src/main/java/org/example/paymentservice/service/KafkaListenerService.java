package org.example.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.PaymentRequestedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


/**
 * Service listens of the Kafka queue and forwards messages for processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaListenerService {

    private final PaymentService paymentService;

    /**
     * Listens and forwards processing of {@link PaymentRequestedEvent}
     *
     * @param event Event to process
     */
    @KafkaListener(topics = EventConstants.TOPIC_PAYMENT_REQUESTED)
    public void handlePaymentRequested(PaymentRequestedEvent event) {
        log.info("[PAYMENT-SERVICE][KAFKA-LISTENER] Received event {} for processing.", event.eventId());
        paymentService.processPayment(event);
    }

    @KafkaListener(topics = EventConstants.TOPIC_PAYMENT_REQUESTED_DLT)
    public void handlePaymentRequestedDLT(String message) {
        log.warn("[PAYMENT-SERVICE][KAFKA-LISTENER] Received DLT message for event {}", EventConstants.EVENT_PAYMENT_REQUESTED);
    }

}
