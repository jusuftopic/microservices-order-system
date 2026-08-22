package com.example.investigationservice.config.kafka;

import com.example.investigationservice.exception.InvalidLifecycleEventException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.example.messagingstarter.EventConstants;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Defines retry and recovery behavior for lifecycle evidence consumption.
 */
@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaReliabilityConfig {

    private static final long RETRY_INTERVAL_MILLIS = 2_000L;
    private static final long RETRY_ATTEMPTS = 3L;

    /**
     * Creates the service-owned dead-letter topic for lifecycle records that
     * cannot be processed safely.
     *
     * @param properties topic infrastructure settings
     * @return dead-letter topic definition
     */
    @Bean
    public NewTopic investigationOrderLifecycleDlt(KafkaTopicProperties properties) {
        return TopicBuilder.name(EventConstants.TOPIC_INVESTIGATION_ORDER_LIFECYCLE_DLT)
                .partitions(properties.partitions())
                .replicas(properties.replicas())
                .build();
    }

    /**
     * Retries transient failures and routes exhausted or non-retryable records
     * to the Investigation Service dead-letter topic.
     *
     * @param kafkaTemplate template used to publish recovered records
     * @return listener error handler
     */
    @Bean
    public DefaultErrorHandler investigationKafkaErrorHandler(
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(
                        EventConstants.TOPIC_INVESTIGATION_ORDER_LIFECYCLE_DLT,
                        record.partition()
                )
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(RETRY_INTERVAL_MILLIS, RETRY_ATTEMPTS)
        );
        errorHandler.addNotRetryableExceptions(
                DeserializationException.class,
                InvalidLifecycleEventException.class
        );

        return errorHandler;
    }
}
