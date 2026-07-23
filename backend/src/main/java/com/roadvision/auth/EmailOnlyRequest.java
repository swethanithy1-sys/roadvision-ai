package com.roadvision.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailOnlyRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email
) {
}
