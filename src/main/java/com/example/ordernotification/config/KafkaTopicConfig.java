package com.example.ordernotification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Programmatically creates Kafka topics on application startup.
 * Spring Boot's KafkaAdmin picks these up automatically.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.orders-placed}")
    private String ordersPlacedTopic;

    @Value("${app.kafka.topics.orders-dlq}")
    private String ordersDlqTopic;

    @Bean
    public NewTopic ordersPlacedTopic() {
        return TopicBuilder.name(ordersPlacedTopic)
                .partitions(3)          // 3 partitions = parallel processing
                .replicas(1)            // 1 replica for local dev
                .build();
    }

    @Bean
    public NewTopic ordersDlqTopic() {
        return TopicBuilder.name(ordersDlqTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
