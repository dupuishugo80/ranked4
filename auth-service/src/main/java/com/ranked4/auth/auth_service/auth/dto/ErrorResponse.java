package com.ranked4.auth.auth_service.auth.dto;

public class ErrorResponse {
    private String error;
    
    public ErrorResponse(String error) { this.error = error; }
    public String getError() { return error; }
}
