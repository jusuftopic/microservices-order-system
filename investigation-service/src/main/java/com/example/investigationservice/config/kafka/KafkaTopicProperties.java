package com.example.investigationservice.config.kafka;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Defines the infrastructure settings used when the Investigation Service
 * creates its Kafka topics.
 *
 * @param partitions number of partitions
 * @param replicas replication factor
 */
@Validated
@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicProperties(
        @Min(1) int partitions,
        @Min(1) short replicas
) {
}
