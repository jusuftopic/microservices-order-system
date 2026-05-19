package org.example.orderservice.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.example.commons.event.EventConstants;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic infrastructure definition.
 *
 * <p>This configuration ensures that all required Kafka topics are created
 * explicitly at  application startup using Spring Kafka Admin.</p>
 *
 *
 * <p>Topics are created idempotently (safe to redeploy).</p>
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaTopicConfig {

    /**
     * Creates the topic used to publish inventory-check event.
     *
     * <p>This topic is part of the inventory workflow and is consumed by
     * the inventory-service.</p>
     */
    @Bean
    public NewTopic orderInventoryRequestedV1Topic(KafkaTopicProperties props) {
        return TopicBuilder.name(EventConstants.TOPIC_ORDER_INVENTORY_REQUEST_V1)
                .partitions(props.partitions())
                .replicas(props.replicas())
                .build();
    }

    /**
     * Creates the topic used to publish payment initiation events.
     *
     * <p>This topic is part of the payment workflow and is consumed by
     * the payment-service.</p>
     */
    @Bean
    public NewTopic paymentRequestedV1Topic(KafkaTopicProperties props) {
        return TopicBuilder.name(EventConstants.TOPIC_ODER_PAYMENT_REQUEST_V1)
                .partitions(props.partitions())
                .replicas(props.replicas())
                .build();
    }
}
