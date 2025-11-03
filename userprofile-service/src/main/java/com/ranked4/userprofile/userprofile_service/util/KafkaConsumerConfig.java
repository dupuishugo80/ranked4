package com.ranked4.userprofile.userprofile_service.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

import com.ranked4.userprofile.userprofile_service.dto.GameFinishedEvent;
import com.ranked4.userprofile.userprofile_service.dto.UserRegisteredEvent;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, UserRegisteredEvent> userRegisteredConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        JsonDeserializer<UserRegisteredEvent> deserializer = new JsonDeserializer<>(UserRegisteredEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("com.ranked4.userprofile.userprofile_service.dto", "com.ranked4.auth.auth_service.authdto");
        deserializer.setUseTypeMapperForKey(true);
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserRegisteredEvent> userRegisteredKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserRegisteredEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userRegisteredConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, GameFinishedEvent> gameFinishedConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        JsonDeserializer<GameFinishedEvent> deserializer = new JsonDeserializer<>(GameFinishedEvent.class);
        deserializer.setUseTypeHeaders(false);
        deserializer.addTrustedPackages("com.ranked4.game.game_service.dto", "com.ranked4.userprofile.userprofile_service.dto");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, GameFinishedEvent> gameFinishedListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, GameFinishedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(gameFinishedConsumerFactory());
        return factory;
    }
}