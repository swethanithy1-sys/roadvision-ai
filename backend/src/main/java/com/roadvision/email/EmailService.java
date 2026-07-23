package com.roadvision.email;

/**
 * Sends transactional auth emails. {@link LogEmailServiceImpl} (default) just logs the link for
 * local development; {@link ResendEmailServiceImpl} (opt-in) sends real email via Resend.
 */
public interface EmailService {

    void sendVerificationEmail(String toEmail, String toName, String verificationLink);

    void sendPasswordResetEmail(String toEmail, String toName, String resetLink);
}
