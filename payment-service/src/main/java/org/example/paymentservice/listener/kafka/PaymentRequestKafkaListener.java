package org.example.paymentservice.listener.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.PaymentRequestedEvent;
import org.example.paymentservice.service.PaymentService;
import org.example.paymentservice.utils.Constants;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


/**
 * Service listens to the primary payment queue and forwards messages for processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
@KafkaListener(
        topics = EventConstants.TOPIC_ODER_PAYMENT_REQUEST_V1,
        groupId = Constants.KAFKA_PAYMENT_GROUP_ID
)
public class PaymentRequestKafkaListener {

    private final PaymentService paymentService;

    /**
     * Listens and forwards processing of {@link PaymentRequestedEvent}
     *
     * @param event Event to process
     */
    @KafkaHandler
    public void handlePaymentRequested(PaymentRequestedEvent event) {
        log.info("[PAYMENT-SERVICE][KAFKA-LISTENER] Received payment request for order {} for processing. Correlation ID {}",
                event.orderId(), event.correlationId());

        paymentService.processPayment(event);
    }

    /**
     * Catch-all fallback method to capture unmapped data shapes safely without breaking consumers
     *
     * @param unknownMessage Unknown message
     */
    @KafkaHandler(isDefault = true)
    public void handleUnknownObject(Object unknownMessage) {
        log.error("[PAYMENT-SERVICE][KAFKA-LISTENER] Unmatched event signature fallback triggered! Payload object type: {}",
                unknownMessage.getClass().getName());
    }
}
