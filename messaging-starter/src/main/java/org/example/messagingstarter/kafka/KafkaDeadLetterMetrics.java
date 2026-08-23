package org.example.messagingstarter.kafka;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.kafka.support.serializer.DeserializationException;

/**
 * Records successful Kafka dead-letter publications with bounded labels.
 */
final class KafkaDeadLetterMetrics {

    static final String DEAD_LETTER_PUBLISHED_METRIC =
            "kafka.consumer.dead.letter.published";

    private static final String DESERIALIZATION_CATEGORY = "deserialization";
    private static final String VALIDATION_CATEGORY = "validation";
    private static final String PROCESSING_CATEGORY = "processing";

    private final MeterRegistry registry;

    KafkaDeadLetterMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Increments the counter after Kafka confirms a dead-letter publication.
     *
     * @param sourceTopic original business topic
     * @param exception failure that caused dead-letter routing
     */
    void recordPublished(String sourceTopic, Exception exception) {
        Counter.builder(DEAD_LETTER_PUBLISHED_METRIC)
                .description("Business records successfully published to a Kafka dead-letter topic")
                .tag("source_topic", sourceTopic)
                .tag("exception_category", exceptionCategory(exception))
                .register(registry)
                .increment();
    }

    private String exceptionCategory(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof DeserializationException) {
                return DESERIALIZATION_CATEGORY;
            }
            if (current instanceof IllegalArgumentException) {
                return VALIDATION_CATEGORY;
            }
            current = current.getCause();
        }
        return PROCESSING_CATEGORY;
    }
}
