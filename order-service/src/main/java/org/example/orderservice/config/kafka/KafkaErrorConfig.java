package org.example.orderservice.config.kafka;

import org.apache.kafka.common.TopicPartition;
import org.example.messagingstarter.EventConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka consumer reliability configuration for order-service.
 *
 * <p>This configuration provides production concerns:
 *
 * <ul>
 *   <li>Centralized listener error handling with retry + DLT routing</li>
 * </ul>
 *
 * <p>This setup avoids poison messages blocking partition consumption and
 * preserves failed events for later investigation or replay.
 */
@Configuration
public class KafkaErrorConfig {

    @Bean
    DefaultErrorHandler errorHandler(
            KafkaTemplate<String,Object> template) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        template,
                        (record, ex) -> new TopicPartition(
                                EventConstants.TOPIC_ORDER_DLQ,
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
