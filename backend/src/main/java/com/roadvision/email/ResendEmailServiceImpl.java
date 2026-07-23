package com.roadvision.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/** Sends real email via Resend's free REST API (no SDK dependency — same RestClient pattern used elsewhere). */
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
    public void sendVerificationEmail(String toEmail, String toName, String verificationLink) {
        send(toEmail, "Verify your RoadVision AI account", EmailTemplates.verificationEmailHtml(toName, verificationLink));
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String toName, String resetLink) {
        send(toEmail, "Reset your RoadVision AI password", EmailTemplates.passwordResetEmailHtml(toName, resetLink));
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
