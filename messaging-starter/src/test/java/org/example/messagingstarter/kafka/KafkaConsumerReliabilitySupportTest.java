package org.example.messagingstarter.kafka;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaConsumerReliabilitySupportTest {

    @Test
    void terminalDeadLetterHandlerRecoversWithoutAnotherPublication() {
        DefaultErrorHandler errorHandler =
                KafkaConsumerReliabilitySupport.terminalDeadLetterErrorHandler();
        ConsumerRecord<String, Object> record =
                new ConsumerRecord<>("service.dlq", 0, 12L, "order-42", new Object());

        boolean recovered = errorHandler.handleOne(
                new IllegalStateException("observer failed"),
                record,
                null,
                null
        );

        assertThat(recovered).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void deadLetterListenerFactoryReplacesJsonDeserializationWithRawBytes() {
        ConcurrentKafkaListenerContainerFactoryConfigurer configurer =
                mock(ConcurrentKafkaListenerContainerFactoryConfigurer.class);
        ConsumerFactory<Object, Object> sourceConsumerFactory = mock(ConsumerFactory.class);
        Map<String, Object> sourceProperties = new HashMap<>();
        sourceProperties.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class
        );
        sourceProperties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class
        );
        sourceProperties.put(
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                JsonDeserializer.class
        );
        when(sourceConsumerFactory.getConfigurationProperties()).thenReturn(sourceProperties);

        KafkaConsumerReliabilitySupport.deadLetterListenerFactory(
                configurer,
                sourceConsumerFactory
        );

        ArgumentCaptor<ConsumerFactory<Object, Object>> consumerFactoryCaptor =
                ArgumentCaptor.forClass(ConsumerFactory.class);
        verify(configurer).configure(
                any(ConcurrentKafkaListenerContainerFactory.class),
                consumerFactoryCaptor.capture()
        );

        Map<String, Object> deadLetterProperties =
                consumerFactoryCaptor.getValue().getConfigurationProperties();
        assertThat(deadLetterProperties)
                .containsEntry(
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        StringDeserializer.class
                )
                .containsEntry(
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        ByteArrayDeserializer.class
                )
                .doesNotContainKey(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS);
    }

    @Test
    @SuppressWarnings("unchecked")
    void successfulDeadLetterPublicationIncrementsMetric() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        SendResult<Object, Object> successfulSend = mock(SendResult.class);
        DefaultErrorHandler errorHandler = deadLetterErrorHandler(
                registry,
                CompletableFuture.completedFuture(successfulSend)
        );

        boolean recovered = errorHandler.handleOne(
                new IllegalStateException("processing failed"),
                new ConsumerRecord<>("order.payment.request.v1", 0, 12L, "order-42", new Object()),
                null,
                null
        );

        assertThat(recovered).isTrue();
        assertThat(registry.get(KafkaDeadLetterMetrics.DEAD_LETTER_PUBLISHED_METRIC)
                .tag("source_topic", "order.payment.request.v1")
                .tag("exception_category", "processing")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    void failedDeadLetterPublicationDoesNotIncrementMetric() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        DefaultErrorHandler errorHandler = deadLetterErrorHandler(
                registry,
                CompletableFuture.failedFuture(new KafkaException("broker unavailable"))
        );

        boolean recovered = errorHandler.handleOne(
                new IllegalArgumentException("invalid payload"),
                new ConsumerRecord<>("order.inventory.request.v1", 0, 15L, "order-42", new Object()),
                null,
                null
        );

        assertThat(recovered).isFalse();
        assertThat(registry.find(KafkaDeadLetterMetrics.DEAD_LETTER_PUBLISHED_METRIC)
                .counter()).isNull();
    }

    @SuppressWarnings("unchecked")
    private DefaultErrorHandler deadLetterErrorHandler(
            SimpleMeterRegistry registry,
            CompletableFuture<SendResult<Object, Object>> sendResult
    ) {
        KafkaOperations<Object, Object> kafkaOperations = mock(KafkaOperations.class);
        when(kafkaOperations.send(any(ProducerRecord.class))).thenReturn(sendResult);

        return KafkaConsumerReliabilitySupport.deadLetterErrorHandler(
                kafkaOperations,
                registry,
                "service.dlq",
                0L,
                0L,
                IllegalArgumentException.class
        );
    }
}
