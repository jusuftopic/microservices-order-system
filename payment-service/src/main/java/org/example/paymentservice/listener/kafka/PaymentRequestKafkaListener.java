package org.example.paymentservice.listener.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.contracts.commands.RefundPaymentCommand;
import org.example.messagingstarter.contracts.commands.ProcessPaymentCommand;
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
        topics = EventConstants.TOPIC_ORDER_PAYMENT_REQUEST_V1,
        groupId = Constants.KAFKA_PAYMENT_GROUP_ID
)
public class PaymentRequestKafkaListener {

    private final PaymentService paymentService;

    /**
     * Listens and forwards processing of {@link ProcessPaymentCommand}
     *
     * @param event Event to process
     */
    @KafkaHandler
    public void handlePaymentRequested(ProcessPaymentCommand event) {
        log.info("[PAYMENT-SERVICE][KAFKA-LISTENER] Received payment request for order {} for processing. Correlation ID {}",
                event.orderId(), event.correlationId());

        paymentService.processPayment(event);
    }


    /**
     * Handles incoming payment refund requests.
     *
     * <p>
     * This event represents a compensation step in the order workflow.
     * It is triggered when a previously successful payment needs to be refunded,
     * typically due to a failure in downstream processing (e.g. inventory commit failure).
     * </p>
     *
     * @param event payment refund request event
     */
    @KafkaHandler
    public void handlePaymentRefundRequested(
            RefundPaymentCommand event
    ) {
        log.warn(
                "[PAYMENT-SERVICE][KAFKA-LISTENER] Received payment refund request for order {} correlationId {}",
                event.orderId(),
                event.correlationId()
        );

        paymentService.handleRefund(event);
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
