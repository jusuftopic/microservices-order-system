package org.example.paymentservice.config;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.example.commons.event.PaymentRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

/**
 * Kafka consumer configurations
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    ConsumerFactory<String, PaymentRequestedEvent> consumerFactory() {
        Map<String,Object> props = new HashMap<>();

        props.put(GROUP_ID_CONFIG, "payment-group");

        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        /* It wraps JSON Deserializer and converts failures into
        * Spring-manageable exceptions so DefaultErrorHandler can process them
        */
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        props.put(
                ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                JsonDeserializer.class
        );

        /**
         * Tells JsonDeserializer which class to deserialize into when
         * type headers are absent.
         */
        props.put(
                JsonDeserializer.VALUE_DEFAULT_TYPE,
                PaymentRequestedEvent.class.getName()
        );

        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        return new DefaultKafkaConsumerFactory<>(props);
    }

}
