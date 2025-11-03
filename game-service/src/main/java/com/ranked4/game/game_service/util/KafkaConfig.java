package com.ranked4.game.game_service.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.ranked4.game.game_service.dto.MatchFoundEvent;

@EnableKafka
@Configuration
public class KafkaConfig {

    public static final String GAME_FINISHED_TOPIC = "game.finished";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, MatchFoundEvent> matchFoundConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        JsonDeserializer<MatchFoundEvent> deserializer = new JsonDeserializer<>(MatchFoundEvent.class);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("com.ranked4.game.dto");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MatchFoundEvent> matchFoundListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, MatchFoundEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(matchFoundConsumerFactory());
        return factory;
    }

    @Bean
    public NewTopic gameFinishedTopic() {
        return TopicBuilder.name(GAME_FINISHED_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}