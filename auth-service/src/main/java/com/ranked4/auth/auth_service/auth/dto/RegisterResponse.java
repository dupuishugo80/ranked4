package com.ranked4.auth.auth_service.auth.dto;

public class RegisterResponse {
    private String message;
    
    public RegisterResponse(String message) { this.message = message; }
    public String getMessage() { return message; }
}
