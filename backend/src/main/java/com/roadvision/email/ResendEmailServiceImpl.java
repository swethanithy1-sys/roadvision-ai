package com.roadvision.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Sends the one-time codes via Resend's REST API. Deliberately the REST API and not SMTP:
 * Resend's SMTP relay requires a verified sending domain, whereas the REST API works with the
 * shared onboarding@resend.dev sender for testing.
 */
@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
@Slf4j
public class ResendEmailServiceImpl implements EmailService {

    private final RestClient resendRestClient;
    private final String apiKey;
    private final String fromAddress;

    public ResendEmailServiceImpl(
            RestClient resendRestClient,
            @Value("${app.email.resend.api-key}") String apiKey,
            @Value("${app.email.resend.from}") String fromAddress
    ) {
        this.resendRestClient = resendRestClient;
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendVerificationCode(String toEmail, String toName, String code) {
        send(toEmail, "Your RoadVision AI verification code", EmailTemplates.verificationCodeHtml(toName, code));
    }

    @Override
    public void sendPasswordResetCode(String toEmail, String toName, String code) {
        send(toEmail, "Your RoadVision AI password reset code", EmailTemplates.passwordResetCodeHtml(toName, code));
    }

    private void send(String toEmail, String subject, String html) {
        ResendEmailRequest request = new ResendEmailRequest(fromAddress, List.of(toEmail), subject, html);
        resendRestClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer " + apiKey)
                .body(request)
                .retrieve()
                .toBodilessEntity();
        log.info("Sent '{}' email to {}", subject, toEmail);
    }
}
