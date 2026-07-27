package com.roadvision.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roadvision.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Calls Supabase Auth's REST API directly from the backend, rather than the frontend talking to
 * Supabase itself — the frontend only ever calls our own /auth/* endpoints.
 * <p>
 * One-time codes are minted through the Admin {@code generate_link} endpoint, which returns the
 * plaintext {@code email_otp} <em>without</em> sending anything, so the app can deliver it via its
 * own {@link com.roadvision.email.EmailService}. Supabase's built-in mailer is bypassed on purpose:
 * its email templates are only editable once custom SMTP is configured, and its default templates
 * send a confirmation <em>link</em> rather than the code this app's UI asks the user to type.
 */
@Service
@Slf4j
public class SupabaseAuthService {

    private static final String TYPE_SIGNUP = "signup";
    private static final String TYPE_RECOVERY = "recovery";

    private final RestClient authClient;
    private final String publishableKey;
    private final String secretKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SupabaseAuthService(
            RestClient supabaseAuthRestClient,
            @Value("${app.supabase.publishable-key}") String publishableKey,
            @Value("${app.supabase.secret-key}") String secretKey
    ) {
        this.authClient = supabaseAuthRestClient;
        this.publishableKey = publishableKey;
        this.secretKey = secretKey;
    }

    /**
     * Creates the (unconfirmed) account and returns its one-time verification code. The password
     * set here is a throwaway — the user picks their real one at the end of the flow, once the
     * code has proven they own the address.
     */
    public String generateSignupOtp(String email, String fullName, String phone) {
        return generateOtp(Map.of(
                "type", TYPE_SIGNUP,
                "email", email,
                "password", UUID.randomUUID().toString(),
                "data", Map.of("full_name", fullName, "phone", phone == null ? "" : phone)
        ));
    }

    /** Returns a one-time code for resetting an existing account's password. */
    public String generateRecoveryOtp(String email) {
        return generateOtp(Map.of("type", TYPE_RECOVERY, "email", email));
    }

    @SuppressWarnings("unchecked")
    private String generateOtp(Map<String, Object> body) {
        Map<String, Object> response = call(() -> authClient.post()
                .uri("/admin/generate_link")
                .header("apikey", secretKey)
                .header("Authorization", "Bearer " + secretKey)
                .body(body)
                .retrieve()
                .body(Map.class));

        String otp = (String) response.get("email_otp");
        if (otp == null) {
            throw new BadRequestException("Supabase did not return a verification code.");
        }
        return otp;
    }

    public OtpResult verifySignupOtp(String email, String otp) {
        return verify(email, otp, TYPE_SIGNUP);
    }

    public OtpResult verifyRecoveryOtp(String email, String otp) {
        return verify(email, otp, TYPE_RECOVERY);
    }

    @SuppressWarnings("unchecked")
    private OtpResult verify(String email, String otp, String type) {
        Map<String, Object> response = call(() -> authClient.post()
                .uri("/verify")
                .header("apikey", publishableKey)
                .body(Map.of("email", email, "token", otp, "type", type))
                .retrieve()
                .body(Map.class));

        Map<String, Object> user = (Map<String, Object>) response.get("user");
        return new OtpResult((String) response.get("access_token"), (String) user.get("id"));
    }

    public void setPassword(String accessToken, String password) {
        call(() -> authClient.put()
                .uri("/user")
                .header("apikey", publishableKey)
                .header("Authorization", "Bearer " + accessToken)
                .body(Map.of("password", password))
                .retrieve()
                .toBodilessEntity());
    }

    @SuppressWarnings("unchecked")
    public SignInResult signIn(String email, String password) {
        Map<String, Object> response;
        try {
            response = authClient.post()
                    .uri("/token?grant_type=password")
                    .header("apikey", publishableKey)
                    .body(Map.of("email", email, "password", password))
                    .retrieve()
                    .body(Map.class);
        } catch (HttpClientErrorException ex) {
            // Supabase answers bad credentials with a 400; surface it as a 401 like any other
            // failed sign-in rather than letting it look like a malformed request.
            throw new BadCredentialsException(extractErrorMessage(ex));
        }

        Map<String, Object> user = (Map<String, Object>) response.get("user");
        return new SignInResult((String) response.get("access_token"), (String) user.get("id"));
    }

    private <T> T call(Supplier<T> action) {
        try {
            return action.get();
        } catch (HttpClientErrorException ex) {
            throw new BadRequestException(extractErrorMessage(ex));
        }
    }

    @SuppressWarnings("unchecked")
    private String extractErrorMessage(HttpClientErrorException ex) {
        try {
            Map<String, Object> json = objectMapper.readValue(ex.getResponseBodyAsString(), Map.class);
            if (json.get("msg") != null) return String.valueOf(json.get("msg"));
            if (json.get("error_description") != null) return String.valueOf(json.get("error_description"));
            if (json.get("error") != null) return String.valueOf(json.get("error"));
        } catch (Exception parseError) {
            log.warn("Could not parse Supabase Auth error response", parseError);
        }
        return "Authentication request failed.";
    }

    public record OtpResult(String accessToken, String userId) {
    }

    public record SignInResult(String accessToken, String userId) {
    }
}
