package com.roadvision.auth;

/** A short-lived Supabase access token, valid but not yet a "real" login — the account still has no password until {@link SetPasswordRequest} completes it. */
public record VerifyOtpResponse(String accessToken) {
}
