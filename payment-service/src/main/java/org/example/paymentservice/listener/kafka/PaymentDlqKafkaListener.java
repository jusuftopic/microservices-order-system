package org.example.paymentservice.listener.kafka;

import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.EventConstants;
import org.example.paymentservice.utils.Constants;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Service dedicated to monitoring and handling the Payment Dead Letter Queue (DLQ)
 */
@Service
@Slf4j
public class PaymentDlqKafkaListener {

    /**
     * Processes raw failed payloads forwarded to the DLT/DLQ
     *
     * @param message Raw message string
     */
    @KafkaListener(
            topics = EventConstants.TOPIC_PAYMENT_DLQ,
            groupId = Constants.KAFKA_PAYMENT_GROUP_ID
    )
    public void handlePaymentRequestedDLT(String message) {
        log.warn("[PAYMENT-SERVICE][DLQ-LISTENER] Received DLT message for event {}. Raw Payload: {}",
                EventConstants.EVENT_PAYMENT_REQUESTED, message);

        // TODO: Insert alerting, database logging, or manual intervention hooks here
    }
}
