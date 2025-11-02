package com.ranked4.auth.auth_service.auth.dto;

import java.util.UUID;

public class UserRegisteredEvent {

    private UUID userId;
    private String username;
    private String email;

    public UserRegisteredEvent() {
    }

    public UserRegisteredEvent(UUID userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }
    
    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

}
