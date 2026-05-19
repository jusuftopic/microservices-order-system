package org.example.inventoryservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.example.commons.event.EventConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka consumer reliability configuration for inventory-service.
 *
 * <p>This configuration provides two production concerns:
 *
 * <ul>
 *   <li>Creation of the main topic dead-letter topic (DLT)</li>
 *   <li>Centralized listener error handling with retry + DLT routing</li>
 * </ul>
 *
 * <p>This setup avoids poison messages blocking partition consumption and
 * preserves failed events for later investigation or replay.
 */
@Configuration
public class KafkaErrorConfig {

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
    public NewTopic inventoryRequestedDltTopic() {
        return TopicBuilder.name(EventConstants.TOPIC_INVENTORY_DLQ)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    DefaultErrorHandler errorHandler(
            KafkaTemplate<Object,Object> template) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        template,
                        (record, ex) -> new TopicPartition(
                                EventConstants.TOPIC_INVENTORY_DLQ,
                                record.partition()
                        )
                );

        FixedBackOff backoff =
                new FixedBackOff(2000L, 3);

        DefaultErrorHandler handler =
                new DefaultErrorHandler(recoverer, backoff);

        handler.addNotRetryableExceptions(
                DeserializationException.class,
                IllegalArgumentException.class
        );

        return handler;
    }
}
