package com.roadvision.auth;

import com.roadvision.common.constants.Role;
import com.roadvision.common.exception.BadRequestException;
import com.roadvision.common.exception.DuplicateResourceException;
import com.roadvision.common.exception.EmailNotVerifiedException;
import com.roadvision.email.EmailService;
import com.roadvision.security.JwtService;
import com.roadvision.security.UserPrincipal;
import com.roadvision.user.User;
import com.roadvision.user.UserMapper;
import com.roadvision.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofHours(24);
    private static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        // Registration is always CITIZEN; admin accounts are provisioned separately (seed data / future admin console).
        User user = new User(
                request.fullName(),
                request.email().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.phone(),
                Role.CITIZEN
        );
        user = userRepository.save(user);

        issueAndSendVerificationEmail(user);

        return new RegisterResponse(user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
        );

        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Please verify your email before logging in");
        }

        return issueAuthResponse(user);
    }

    @Transactional
    public AuthResponse verifyEmail(String token) {
        AuthToken authToken = authTokenRepository.findByTokenAndType(token, TokenType.EMAIL_VERIFICATION)
                .filter(AuthToken::isValid)
                .orElseThrow(() -> new BadRequestException("This verification link is invalid or has expired"));

        User user = authToken.getUser();
        user.setEmailVerified(true);
        authToken.setUsedAt(Instant.now());

        return issueAuthResponse(user);
    }

    @Transactional
    public void resendVerification(String email) {
        // Always succeeds from the caller's perspective — doesn't reveal whether the account exists.
        userRepository.findByEmail(email.toLowerCase())
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::issueAndSendVerificationEmail);
    }

    @Transactional
    public void forgotPassword(String email) {
        // Always succeeds from the caller's perspective — doesn't reveal whether the account exists.
        userRepository.findByEmail(email.toLowerCase()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            authTokenRepository.save(new AuthToken(user, token, TokenType.PASSWORD_RESET, Instant.now().plus(RESET_TOKEN_TTL)));
            String resetLink = frontendUrl + "/reset-password?token=" + token;
            trySendEmail(() -> emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetLink));
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        AuthToken authToken = authTokenRepository.findByTokenAndType(token, TokenType.PASSWORD_RESET)
                .filter(AuthToken::isValid)
                .orElseThrow(() -> new BadRequestException("This password reset link is invalid or has expired"));

        User user = authToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        authToken.setUsedAt(Instant.now());
    }

    private void issueAndSendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();
        authTokenRepository.save(new AuthToken(user, token, TokenType.EMAIL_VERIFICATION, Instant.now().plus(VERIFICATION_TOKEN_TTL)));
        String verificationLink = frontendUrl + "/verify-email?token=" + token;
        trySendEmail(() -> emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), verificationLink));
    }

    /** An email provider hiccup should never break registration/reset — the user can always request a resend. */
    private void trySendEmail(Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            log.error("Failed to send auth email", ex);
        }
    }

    private AuthResponse issueAuthResponse(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);
        return new AuthResponse(token, userMapper.toResponse(user));
    }
}
