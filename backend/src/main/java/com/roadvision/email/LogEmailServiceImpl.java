package com.roadvision.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Free default: prints the code instead of sending real email, so local dev needs no API key. */
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "log", matchIfMissing = true)
@Slf4j
public class LogEmailServiceImpl implements EmailService {

    @Override
    public void sendVerificationCode(String toEmail, String toName, String code) {
        log.info("[DEV EMAIL] Verification code for {} <{}>: {}", toName, toEmail, code);
    }

    @Override
    public void sendPasswordResetCode(String toEmail, String toName, String code) {
        log.info("[DEV EMAIL] Password reset code for {} <{}>: {}", toName, toEmail, code);
    }
}
