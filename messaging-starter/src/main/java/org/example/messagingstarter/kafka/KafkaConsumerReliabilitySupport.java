package org.example.messagingstarter.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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
     * Creates a terminal listener factory which reads dead-letter values as
     * raw bytes. This allows records whose domain deserialization failed to be
     * observed without attempting the same deserialization again.
     *
     * @param configurer Spring Boot listener-factory configurer
     * @param sourceConsumerFactory source of the service's Kafka connection settings
     * @return listener factory dedicated to dead-letter records
     */
    public static ConcurrentKafkaListenerContainerFactory<Object, Object> deadLetterListenerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> sourceConsumerFactory
    ) {
        Map<String, Object> consumerProperties =
                new HashMap<>(sourceConsumerFactory.getConfigurationProperties());
        consumerProperties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );
        consumerProperties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ByteArrayDeserializer.class
        );
        consumerProperties.remove(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS);
        consumerProperties.remove(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS);

        ConsumerFactory<Object, Object> deadLetterConsumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProperties);

        return listenerFactory(
                configurer,
                deadLetterConsumerFactory,
                terminalDeadLetterErrorHandler()
        );
    }

    /**
     * Creates a bounded retry policy which publishes unrecoverable records to
     * the service-owned dead-letter topic.
     *
     * @param kafkaOperations Kafka publisher used by the recoverer
     * @param meterRegistry registry receiving dead-letter publication metrics
     * @param deadLetterTopic service-owned terminal topic
     * @param retryIntervalMillis delay between processing attempts
     * @param retryAttempts number of retries after the initial attempt
     * @param notRetryableExceptions failures routed directly to dead letter
     * @return business-listener error handler
     */
    @SafeVarargs
    public static DefaultErrorHandler deadLetterErrorHandler(
            KafkaOperations<?, ?> kafkaOperations,
            MeterRegistry meterRegistry,
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
        recoverer.setFailIfSendResultIsError(true);

        KafkaDeadLetterMetrics metrics = new KafkaDeadLetterMetrics(meterRegistry);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (record, exception) -> {
                    recoverer.accept(record, exception);
                    try {
                        metrics.recordPublished(record.topic(), exception);
                    } catch (RuntimeException metricsException) {
                        log.error(
                                "[MESSAGING-STARTER][KAFKA-DLQ] Failed to record successful dead-letter publication from {}-{}@{}",
                                record.topic(),
                                record.partition(),
                                record.offset(),
                                metricsException
                        );
                    }
                },
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

    /**
     * Logs the diagnostic metadata attached to a terminal dead-letter record.
     * The payload itself is not logged because it may contain sensitive data.
     *
     * @param serviceName service observing the record
     * @param record terminal dead-letter record
     */
    public static void logTerminalDeadLetter(
            String serviceName,
            ConsumerRecord<String, byte[]> record
    ) {
        log.warn(
                "[{}][KAFKA-DLQ] Observed terminal record {}-{}@{}; "
                        + "originalRecord {}-{}@{}; exceptionType {}; causeType {}; "
                        + "message {}; payloadBytes {}",
                serviceName,
                record.topic(),
                record.partition(),
                record.offset(),
                stringHeader(record, KafkaHeaders.DLT_ORIGINAL_TOPIC),
                integerHeader(record, KafkaHeaders.DLT_ORIGINAL_PARTITION),
                longHeader(record, KafkaHeaders.DLT_ORIGINAL_OFFSET),
                stringHeader(record, KafkaHeaders.DLT_EXCEPTION_FQCN),
                stringHeader(record, KafkaHeaders.DLT_EXCEPTION_CAUSE_FQCN),
                stringHeader(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE),
                record.value() == null ? 0 : record.value().length
        );

        String failureStackTrace = nullableStringHeader(
                record,
                KafkaHeaders.DLT_EXCEPTION_STACKTRACE
        );
        if (failureStackTrace != null) {
            log.warn(
                    "[{}][KAFKA-DLQ] Original failure stack trace for {}-{}@{}:\n{}",
                    serviceName,
                    stringHeader(record, KafkaHeaders.DLT_ORIGINAL_TOPIC),
                    integerHeader(record, KafkaHeaders.DLT_ORIGINAL_PARTITION),
                    longHeader(record, KafkaHeaders.DLT_ORIGINAL_OFFSET),
                    failureStackTrace
            );
        }
    }

    private static String stringHeader(ConsumerRecord<?, ?> record, String headerName) {
        String value = nullableStringHeader(record, headerName);
        return value == null ? "unknown" : value;
    }

    private static String nullableStringHeader(
            ConsumerRecord<?, ?> record,
            String headerName
    ) {
        Header header = record.headers().lastHeader(headerName);
        return header == null || header.value() == null
                ? null
                : new String(header.value(), StandardCharsets.UTF_8);
    }

    private static String integerHeader(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        return header == null || header.value().length != Integer.BYTES
                ? "unknown"
                : Integer.toString(ByteBuffer.wrap(header.value()).getInt());
    }

    private static String longHeader(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        return header == null || header.value().length != Long.BYTES
                ? "unknown"
                : Long.toString(ByteBuffer.wrap(header.value()).getLong());
    }
}
