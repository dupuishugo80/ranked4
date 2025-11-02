package com.ranked4.auth.auth_service.auth.util;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaProducerConfig {

    public static final String TOPIC_USER_REGISTERED = "user.registered";

    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name(TOPIC_USER_REGISTERED)
                .partitions(1)
                .replicas(1)
                .build();
    }
}