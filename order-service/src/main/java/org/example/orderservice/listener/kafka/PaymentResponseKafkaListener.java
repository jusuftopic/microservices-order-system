package org.example.orderservice.listener.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.example.commons.event.contracts.PaymentCompletedEvent;
import org.example.commons.event.contracts.PaymentFailedEvent;
import org.example.orderservice.service.OrderService;
import org.example.orderservice.utils.Constants;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka listener responsible for handling incoming events
 * from the payment service.
 *
 * <p>
 * This component represents the entry point for payment-related
 * orchestration flow in the Order Service.
 * It receives payment processing results and delegates
 * further business handling to the OrderService.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@KafkaListener(
        topics = EventConstants.TOPIC_ORDER_PAYMENT_RESPONSE_V1,
        groupId = Constants.KAFKA_ORDER_GROUP_ID
)
public class PaymentResponseKafkaListener {

    private final OrderService orderService;

    /**
     * Handles successful payment processing event.
     *
     * @param completed successful payment event
     */
    @KafkaHandler
    public void handlePaymentCompleted(PaymentCompletedEvent completed) {

        log.info(
                "[ORDER-SERVICE][KAFKA] Received PaymentCompletedEvent for order {} correlationId {}",
                completed.orderId(),
                completed.correlationId()
        );

        orderService.handlePaymentCompleted(completed);
    }

    /**
     * Handles failed payment processing event.
     *
     * @param failed failed payment event
     */
    @KafkaHandler
    public void handlePaymentFailed(PaymentFailedEvent failed) {

        log.warn(
                "[ORDER-SERVICE][KAFKA] Received PaymentFailedEvent for order {} reason {} correlationId {}",
                failed.orderId(),
                failed.reason(),
                failed.correlationId()
        );

        orderService.handlePaymentFailed(failed);
    }

    /**
     * Catch-all fallback method to capture unmapped data shapes safely
     * without breaking consumers.
     *
     * @param unknownMessage unknown message
     */
    @KafkaHandler(isDefault = true)
    public void handleUnknownObject(Object unknownMessage) {

        log.error(
                "[ORDER-SERVICE][KAFKA] Unmatched payment event signature fallback triggered! Payload object type: {}",
                unknownMessage.getClass().getName()
        );
    }

}
