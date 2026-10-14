package com.fintech.mco1.dto;

/**
 * Payload for authentication responses from the API.
 */
public class AuthResponse {
    private String token;
    private String tokenType;
    private long expiresInSeconds;
    private UserResponse user;

    public AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresInSeconds = expiresInSeconds;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public UserResponse getUser() {
        return user;
    }
}
