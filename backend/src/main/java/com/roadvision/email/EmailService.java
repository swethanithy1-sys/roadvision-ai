package com.roadvision.email;

/**
 * Delivers the one-time codes Supabase generates for us (see
 * {@link com.roadvision.auth.SupabaseAuthService#generateOtp}). Supabase's own mailer is
 * deliberately bypassed: its templates are only editable with custom SMTP configured, and its
 * default templates send a confirmation *link* rather than the code this app's UI asks for.
 * {@link LogEmailServiceImpl} (default) prints the code for local dev; {@link ResendEmailServiceImpl}
 * (opt-in) sends real branded email via Resend's REST API.
 */
public interface EmailService {

    void sendVerificationCode(String toEmail, String toName, String code);

    void sendPasswordResetCode(String toEmail, String toName, String code);
}
