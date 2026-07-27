package com.roadvision.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

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
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        try {
            resendRestClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException ex) {
            // Resend's own message is the useful part (e.g. "verify a domain at resend.com/domains");
            // surface just that rather than dumping the raw status line and JSON body into the UI.
            log.error("Resend rejected the email to {}: {}", toEmail, ex.getResponseBodyAsString());
            throw new EmailDeliveryException(extractMessage(ex));
        }
        log.info("Sent '{}' email to {}", subject, toEmail);
    }

    @SuppressWarnings("unchecked")
    private String extractMessage(HttpClientErrorException ex) {
        try {
            Map<String, Object> json = objectMapper.readValue(ex.getResponseBodyAsString(), Map.class);
            if (json.get("message") != null) {
                return String.valueOf(json.get("message"));
            }
        } catch (Exception parseError) {
            log.warn("Could not parse Resend error response", parseError);
        }
        return "We couldn't send the email. Please try again.";
    }
}
