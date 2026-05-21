package org.example.paymentservice.config;

import lombok.RequiredArgsConstructor;
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
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public Health health() {

        try {
            kafkaTemplate.execute(producer -> {
                producer.partitionsFor(EventConstants.TOPIC_ORDER_INVENTORY_REQUEST_V1);
                return null;
            });

            return Health.up().build();

        } catch (Exception ex) {
            return Health.down(ex).build();
        }

    }
}
