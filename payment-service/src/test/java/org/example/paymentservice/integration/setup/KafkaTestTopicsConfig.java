package org.example.paymentservice.integration.setup;

import org.apache.kafka.clients.admin.NewTopic;
import org.example.commons.event.EventConstants;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@TestConfiguration
public class KafkaTestTopicsConfig {


    @Bean
    public KafkaAdmin.NewTopics topics() {
        return new KafkaAdmin.NewTopics(

                TopicBuilder
                        .name(EventConstants.TOPIC_ORDER_PAYMENT_REQUEST_V1)
                        .partitions(1)
                        .replicas(1)
                        .build()
        );
    }


}
