package com.ranked4.auth.auth_service.auth.dto;

import com.ranked4.auth.auth_service.auth.model.User;

public class ProfileResponse {
    private String username;
    private String email;
    private String roles;

    public ProfileResponse(String username, String email, String roles) {
        this.username = username;
        this.email = email;
        this.roles = roles;
    }

    public ProfileResponse(User user) {
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.roles = user.getRoles().toString();
    }

    public String getUsername() {
        return username;
    }

    public String getRoles() {
        return roles;
    }

    public String getEmail() {
        return email;
    }
}
