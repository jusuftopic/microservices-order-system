package org.example.paymentservice.config;

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
 * Kafka consumer reliability configuration for payment-service.
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
     *
     * <p>Recommended approach because many production Kafka
     * clusters disable automatic topic creation.
     */
    @Bean
    public NewTopic paymentRequestedDltTopic() {
        return TopicBuilder.name(EventConstants.TOPIC_ODER_PAYMENT_REQUEST_V1)
                .partitions(3)
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
                                record.topic() + ".DLT",
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
