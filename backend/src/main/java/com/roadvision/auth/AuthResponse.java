package com.roadvision.auth;

import com.roadvision.user.UserResponse;

public record AuthResponse(String token, UserResponse user) {
}
