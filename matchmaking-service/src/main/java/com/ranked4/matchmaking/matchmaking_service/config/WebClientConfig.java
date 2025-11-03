package com.ranked4.matchmaking.matchmaking_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${user.profile.service.url}")
    private String userProfileServiceUrl;

    @Bean(name = "userProfileWebClient")
    public WebClient userProfileWebClient() {
        return WebClient.builder()
                .baseUrl(userProfileServiceUrl)
                .build();
    }
}
