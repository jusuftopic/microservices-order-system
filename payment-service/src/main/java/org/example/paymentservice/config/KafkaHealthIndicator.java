package org.example.paymentservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.commons.event.EventConstants;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Custom Kafka health indicator.
 *
 * <p>
 * Verifies that Kafka is reachable by attempting to fetch cluster metadata.
 * </p>
 */
@Component("kafka")
@RequiredArgsConstructor
@Slf4j
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public Health health() {

        try {
            kafkaTemplate.execute(producer -> {
                producer.partitionsFor(EventConstants.TOPIC_ORDER_INVENTORY_REQUEST_V1);
                return null;
            });

            log.debug("[PAYMENT-SERVICE][KAFKA] Kafka reports UP status.");
            return Health.up().build();

        } catch (Exception ex) {
            log.warn("[PAYMENT-SERVICE][KAFKA] Kafka reports DOWN status.");
            return Health.down(ex).build();
        }

    }
}
