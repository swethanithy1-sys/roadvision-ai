package com.roadvision.auth;

import com.roadvision.common.exception.DuplicateResourceException;
import com.roadvision.common.exception.ResourceNotFoundException;
import com.roadvision.email.EmailService;
import com.roadvision.user.User;
import com.roadvision.user.UserMapper;
import com.roadvision.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final SupabaseAuthService supabaseAuthService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtDecoder jwtDecoder;

    /** Step 1 of registration: create the unconfirmed account and email its verification code. */
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException(
                    "An account with this email already exists. Try logging in or resetting your password."
            );
        }

        SupabaseAuthService.GeneratedOtp otp =
                supabaseAuthService.generateSignupOtp(email, request.fullName(), request.phone());

        try {
            emailService.sendVerificationCode(email, request.fullName(), otp.code());
        } catch (RuntimeException ex) {
            // The account exists by this point but the user will never receive their code, so it
            // can't be completed — and leaving it behind would make this address permanently
            // un-registerable (the duplicate check above would reject every retry). Roll it back.
            rollBackHalfFinishedSignup(otp.userId(), email);
            throw ex;
        }

        return new RegisterResponse(request.email());
    }

    /** Step 2 of registration: verify the emailed code (no password yet). */
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        SupabaseAuthService.OtpResult otpResult =
                supabaseAuthService.verifySignupOtp(request.email().toLowerCase(), request.otp());
        return new VerifyOtpResponse(otpResult.accessToken());
    }

    /** Step 3 of registration: set the password using the token from {@link #verifyOtp}, completing the account. */
    @Transactional
    public AuthResponse setPassword(SetPasswordRequest request) {
        supabaseAuthService.setPassword(request.accessToken(), request.password());
        String userId = jwtDecoder.decode(request.accessToken()).getSubject();
        return issueAuthResponse(request.accessToken(), userId);
    }

    public AuthResponse login(LoginRequest request) {
        SupabaseAuthService.SignInResult signInResult =
                supabaseAuthService.signIn(request.email().toLowerCase(), request.password());

        return issueAuthResponse(signInResult.accessToken(), signInResult.userId());
    }

    public void forgotPassword(String email) {
        // Always looks identical to the caller whether or not the email is registered.
        userRepository.findByEmail(email.toLowerCase()).ifPresent(user -> {
            try {
                SupabaseAuthService.GeneratedOtp otp = supabaseAuthService.generateRecoveryOtp(user.getEmail());
                emailService.sendPasswordResetCode(user.getEmail(), user.getFullName(), otp.code());
            } catch (Exception ex) {
                // Swallowed on purpose: reporting this would reveal whether the address is registered.
                log.error("Failed to send password reset code", ex);
            }
        });
    }

    public void resetPassword(ResetPasswordRequest request) {
        SupabaseAuthService.OtpResult otpResult =
                supabaseAuthService.verifyRecoveryOtp(request.email().toLowerCase(), request.otp());
        supabaseAuthService.setPassword(otpResult.accessToken(), request.newPassword());
    }

    private void rollBackHalfFinishedSignup(String userId, String email) {
        if (userId == null) {
            log.error("Cannot roll back signup for {} — Supabase returned no user id", email);
            return;
        }
        try {
            supabaseAuthService.deleteUser(userId);
        } catch (Exception cleanupError) {
            log.error("Failed to roll back half-finished signup for {}", email, cleanupError);
        }
    }

    private AuthResponse issueAuthResponse(String accessToken, String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
        return new AuthResponse(accessToken, userMapper.toResponse(user));
    }
}
