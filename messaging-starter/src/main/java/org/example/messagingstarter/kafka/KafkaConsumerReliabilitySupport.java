package org.example.messagingstarter.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Constructs Kafka listener infrastructure shared by the services while
 * leaving topic ownership and exception classification with each service.
 */
@Slf4j
public final class KafkaConsumerReliabilitySupport {

    public static final String BUSINESS_LISTENER_FACTORY =
            "businessKafkaListenerContainerFactory";
    public static final String DEAD_LETTER_LISTENER_FACTORY =
            "deadLetterKafkaListenerContainerFactory";

    private KafkaConsumerReliabilitySupport() {
    }

    /**
     * Creates a listener factory which retains Spring Boot's Kafka settings
     * and applies the supplied error-handling policy.
     *
     * @param configurer Spring Boot listener-factory configurer
     * @param consumerFactory configured Kafka consumer factory
     * @param errorHandler error policy assigned to listeners using the factory
     * @return configured listener container factory
     */
    public static ConcurrentKafkaListenerContainerFactory<Object, Object> listenerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            CommonErrorHandler errorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    /**
     * Creates a bounded retry policy which publishes unrecoverable records to
     * the service-owned dead-letter topic.
     *
     * @param kafkaOperations Kafka publisher used by the recoverer
     * @param deadLetterTopic service-owned terminal topic
     * @param retryIntervalMillis delay between processing attempts
     * @param retryAttempts number of retries after the initial attempt
     * @param notRetryableExceptions failures routed directly to dead letter
     * @return business-listener error handler
     */
    @SafeVarargs
    public static DefaultErrorHandler deadLetterErrorHandler(
            KafkaOperations<?, ?> kafkaOperations,
            String deadLetterTopic,
            long retryIntervalMillis,
            long retryAttempts,
            Class<? extends Exception>... notRetryableExceptions
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, exception) -> new TopicPartition(
                        deadLetterTopic,
                        record.partition()
                )
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(retryIntervalMillis, retryAttempts)
        );
        errorHandler.addNotRetryableExceptions(notRetryableExceptions);
        return errorHandler;
    }

    /**
     * Creates a terminal policy for a dead-letter listener. Failed dead-letter
     * observations are logged and recovered without being republished.
     *
     * @return terminal dead-letter listener error handler
     */
    public static DefaultErrorHandler terminalDeadLetterErrorHandler() {
        return new DefaultErrorHandler(
                (record, exception) -> log.error(
                        "[MESSAGING-STARTER][KAFKA-DLQ] Failed to observe terminal record from {}-{}@{}; record will not be republished",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        exception
                ),
                new FixedBackOff(0L, 0L)
        );
    }
}
