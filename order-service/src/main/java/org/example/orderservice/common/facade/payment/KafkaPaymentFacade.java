package org.example.orderservice.common.facade.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaPaymentFacade implements PaymentFacade {

    @Override
    public void initiatePayment(Long orderId) {
        log.warn("[ORDER-SERVICE][KAFKA] No-opt");
    }
}
