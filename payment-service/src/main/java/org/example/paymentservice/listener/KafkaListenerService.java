package org.example.paymentservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.PaymentRequestedEvent;
import org.example.paymentservice.service.PaymentService;
import org.example.paymentservice.utils.Constants;
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
    @KafkaListener(topics = EventConstants.TOPIC_ODER_PAYMENT_REQUEST_V1,
            groupId = Constants.KAFKA_PAYMENT_GROUP_ID
    )
    public void handlePaymentRequested(PaymentRequestedEvent event) {
        log.info("[PAYMENT-SERVICE][KAFKA-LISTENER] Received payment request for order {} for processing. Correlation ID {}", event.orderId(),
                event.correlationId());
        paymentService.processPayment(event);
    }

    @KafkaListener(topics = EventConstants.TOPIC_PAYMENT_DLQ,
            groupId = Constants.KAFKA_PAYMENT_GROUP_ID)
    public void handlePaymentRequestedDLT(String message) {
        log.warn("[PAYMENT-SERVICE][KAFKA-LISTENER] Received DLT message for event {}", EventConstants.EVENT_PAYMENT_REQUESTED);
    }
}
