package com.ranked4.auth.auth_service.auth.dto;

public class LogoutResponse {
    private String message;
        
    public LogoutResponse(String message) { this.message = message; }
    public String getMessage() { return message; }
}
