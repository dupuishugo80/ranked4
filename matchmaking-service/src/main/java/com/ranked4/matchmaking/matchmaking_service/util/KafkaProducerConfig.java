package com.ranked4.matchmaking.matchmaking_service.util;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaProducerConfig {

    public static final String MATCH_FOUND_TOPIC = "match.found";

    @Bean
    public NewTopic matchFoundTopic() {
        return TopicBuilder.name(MATCH_FOUND_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
