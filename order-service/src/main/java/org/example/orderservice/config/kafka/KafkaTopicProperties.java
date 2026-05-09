package org.example.orderservice.config.kafka;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Kafka topic infrastructure.
 *
 * <p>These values control how topics are created at application startup
 * via Spring Kafka Admin.</p>
 *
 * <p>They are intentionally externalized so they can be overridden per environment:</p>
 *
 * <p>In Kubernetes, these values are typically injected via ConfigMap or environment variables.</p>
 *
 * @param partitions default number of partitions for created topics
 * @param replicas replication factor for created topics
 */
@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicProperties(

        @Min(1)
        int partitions,

        @Min(1)
        short replicas
) {
}
