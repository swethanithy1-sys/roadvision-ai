package com.roadvision.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Free default: prints the link instead of sending real email, so local dev needs no API key. */
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "log", matchIfMissing = true)
@Slf4j
public class LogEmailServiceImpl implements EmailService {

    @Override
    public void sendVerificationEmail(String toEmail, String toName, String verificationLink) {
        log.info("[DEV EMAIL] Verification link for {} <{}>: {}", toName, toEmail, verificationLink);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String toName, String resetLink) {
        log.info("[DEV EMAIL] Password reset link for {} <{}>: {}", toName, toEmail, resetLink);
    }
}
