package org.example.inventoryservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.kafka.KafkaConsumerReliabilitySupport;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;

import static org.example.messagingstarter.kafka.KafkaConsumerReliabilitySupport.BUSINESS_LISTENER_FACTORY;
import static org.example.messagingstarter.kafka.KafkaConsumerReliabilitySupport.DEAD_LETTER_LISTENER_FACTORY;

/**
 * Kafka consumer reliability configuration for inventory-service.
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

    private static final long RETRY_INTERVAL_MILLIS = 2_000L;
    private static final long RETRY_ATTEMPTS = 3L;

    @Bean
    DefaultErrorHandler inventoryKafkaErrorHandler(
            KafkaTemplate<String, Object> template,
            MeterRegistry meterRegistry
    ) {
        return KafkaConsumerReliabilitySupport.deadLetterErrorHandler(
                template,
                meterRegistry,
                EventConstants.TOPIC_INVENTORY_DLQ,
                RETRY_INTERVAL_MILLIS,
                RETRY_ATTEMPTS,
                DeserializationException.class,
                IllegalArgumentException.class
        );
    }

    @Bean(name = BUSINESS_LISTENER_FACTORY)
    ConcurrentKafkaListenerContainerFactory<Object, Object> businessListenerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler inventoryKafkaErrorHandler
    ) {
        return KafkaConsumerReliabilitySupport.listenerFactory(
                configurer,
                consumerFactory,
                inventoryKafkaErrorHandler
        );
    }

    @Bean(name = DEAD_LETTER_LISTENER_FACTORY)
    ConcurrentKafkaListenerContainerFactory<Object, Object> deadLetterListenerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory
    ) {
        return KafkaConsumerReliabilitySupport.listenerFactory(
                configurer,
                consumerFactory,
                KafkaConsumerReliabilitySupport.terminalDeadLetterErrorHandler()
        );
    }
}
