package org.example.notificationservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.admin.NewTopic;
import org.example.messagingstarter.EventConstants;
import org.example.messagingstarter.kafka.KafkaConsumerReliabilitySupport;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;

import static org.example.messagingstarter.kafka.KafkaConsumerReliabilitySupport.BUSINESS_LISTENER_FACTORY;
import static org.example.messagingstarter.kafka.KafkaConsumerReliabilitySupport.DEAD_LETTER_LISTENER_FACTORY;

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaConfig {

    private static final long RETRY_INTERVAL_MILLIS = 2_000L;
    private static final long RETRY_ATTEMPTS = 3L;

    /**
     * Creates the terminal topic for notification records that cannot be
     * processed safely.
     *
     * @param properties topic infrastructure settings
     * @return notification dead-letter topic definition
     */
    @Bean
    public NewTopic notificationDeadLetterTopic(KafkaTopicProperties properties) {
        return TopicBuilder.name(EventConstants.TOPIC_NOTIFICATION_DLQ)
                .partitions(properties.partitions())
                .replicas(properties.replicas())
                .build();
    }

    @Bean
    DefaultErrorHandler notificationKafkaErrorHandler(
            KafkaTemplate<String, Object> kafkaTemplate,
            MeterRegistry meterRegistry
    ) {
        return KafkaConsumerReliabilitySupport.deadLetterErrorHandler(
                kafkaTemplate,
                meterRegistry,
                EventConstants.TOPIC_NOTIFICATION_DLQ,
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
            DefaultErrorHandler notificationKafkaErrorHandler
    ) {
        return KafkaConsumerReliabilitySupport.listenerFactory(
                configurer,
                consumerFactory,
                notificationKafkaErrorHandler
        );
    }

    @Bean(name = DEAD_LETTER_LISTENER_FACTORY)
    ConcurrentKafkaListenerContainerFactory<Object, Object> deadLetterListenerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory
    ) {
        return KafkaConsumerReliabilitySupport.deadLetterListenerFactory(
                configurer,
                consumerFactory
        );
    }
}
