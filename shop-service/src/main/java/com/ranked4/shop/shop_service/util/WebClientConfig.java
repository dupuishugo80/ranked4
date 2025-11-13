package com.ranked4.shop.shop_service.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${userprofile.service.base-url}")
    private String userProfileBaseUrl;

    @Bean
    public WebClient userProfileClient() {
        return WebClient.builder()
                .baseUrl(userProfileBaseUrl)
                .build();
    }
}