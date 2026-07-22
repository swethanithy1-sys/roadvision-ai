package com.roadvision.user;

import com.roadvision.common.constants.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        Role role,
        Instant createdAt
) {
}
