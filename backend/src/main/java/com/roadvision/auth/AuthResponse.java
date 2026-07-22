package com.roadvision.auth;

import com.roadvision.user.UserResponse;

public record AuthResponse(
        String token,
        String tokenType,
        UserResponse user
) {
    public AuthResponse(String token, UserResponse user) {
        this(token, "Bearer", user);
    }
}
