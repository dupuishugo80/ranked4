package com.ranked4.userprofile.userprofile_service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.ranked4.userprofile.userprofile_service.dto.UserRegisteredEvent;
import com.ranked4.userprofile.userprofile_service.service.UserProfileService;

@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);
    private final UserProfileService userProfileService;

    public KafkaConsumerService(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @KafkaListener(
            topics = "user.registered",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "userRegisteredKafkaListenerContainerFactory"
    )
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received user.registered event for user ID: {}", event.getUserId());

        if (event.getUserId() == null || event.getUsername() == null) {
            log.warn("Invalid user.registered message received (missing data): {}", event);
            return;
        }

        try {
            userProfileService.createProfile(event.getUserId(), event.getUsername());
            log.info("Profile created successfully for user ID: {}", event.getUserId());
        } catch (IllegalArgumentException e) {
            log.warn("Unable to create profile (may already exist?): {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error occurred while creating profile for user ID: {}", event.getUserId(), e);
        }
    }
}
