package org.example.inventoryservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.example.messagingstarter.EventConstants;
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
     * Creates the topic used to answer to published inventory-check event.
     *
     * <p>This topic is part of the inventory workflow and is consumed by
     * the order-service.</p>
     */
    @Bean
    public NewTopic orderInventoryResponseV1Topic(KafkaTopicProperties props) {
        return TopicBuilder.name(EventConstants.TOPIC_ORDER_INVENTORY_RESPONSE_V1)
                .partitions(props.partitions())
                .replicas(props.replicas())
                .build();
    }

    /**
     * Creates the topic used to answer to published inventory-finalization event.
     *
     * <p>This topic is part of the inventory workflow and is consumed by
     * the order-service.</p>
     */
    @Bean
    public NewTopic orderInventoryFinalizationResponseV1Topic(KafkaTopicProperties props) {
        return TopicBuilder.name(EventConstants.TOPIC_ORDER_INVENTORY_FINALIZATION_RESPONSE_V1)
                .partitions(props.partitions())
                .replicas(props.replicas())
                .build();
    }

    /**
     * Creates the Dead Letter Topic for failed records.
     *
     * <p>Messages are sent here when:
     *
     * <ul>
     *   <li>deserialization fails</li>
     *   <li>listener processing keeps failing after retries</li>
     * </ul>
     */
    @Bean
    public NewTopic inventoryRequestedDltTopic(KafkaTopicProperties props) {
        return TopicBuilder.name(EventConstants.TOPIC_INVENTORY_DLQ)
                .partitions(props.partitions())
                .replicas(props.replicas())
                .build();
    }
}
