package com.roadvision.email;

/** Branded HTML for transactional auth emails, styled to match the app's theme tokens. */
final class EmailTemplates {

    private EmailTemplates() {
    }

    static String verificationEmailHtml(String name, String verificationLink) {
        return layout(
                "Verify your email",
                "Hi " + escape(name) + ",",
                "Thanks for signing up for RoadVision AI. Confirm your email address to activate "
                        + "your account and start reporting road damage.",
                "Verify email",
                verificationLink,
                "This link expires in 24 hours. If you didn't create this account, you can safely "
                        + "ignore this email."
        );
    }

    static String passwordResetEmailHtml(String name, String resetLink) {
        return layout(
                "Reset your password",
                "Hi " + escape(name) + ",",
                "We received a request to reset your RoadVision AI password. Click the button "
                        + "below to choose a new one.",
                "Reset password",
                resetLink,
                "This link expires in 1 hour. If you didn't request this, you can safely ignore "
                        + "this email — your password won't change."
        );
    }

    private static String layout(String title, String greeting, String body, String buttonText, String buttonLink, String footnote) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background-color:#F8FAFC;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F8FAFC;padding:40px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.08);">
                          <tr>
                            <td style="background-color:#2563EB;padding:24px 32px;">
                              <span style="color:#ffffff;font-size:18px;font-weight:700;letter-spacing:0.02em;">RoadVision AI</span>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <h1 style="margin:0 0 16px;color:#0F172A;font-size:20px;font-weight:700;">%s</h1>
                              <p style="margin:0 0 16px;color:#334155;font-size:15px;line-height:1.6;">%s</p>
                              <p style="margin:0 0 28px;color:#334155;font-size:15px;line-height:1.6;">%s</p>
                              <table role="presentation" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td style="border-radius:8px;background-color:#2563EB;">
                                    <a href="%s" style="display:inline-block;padding:12px 28px;color:#ffffff;font-size:15px;font-weight:600;text-decoration:none;border-radius:8px;">%s</a>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:28px 0 0;color:#64748B;font-size:13px;line-height:1.5;">%s</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:20px 32px;background-color:#F8FAFC;border-top:1px solid #E2E8F0;">
                              <p style="margin:0;color:#94A3B8;font-size:12px;">© RoadVision AI — Smart Road Infrastructure Monitoring</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(title, greeting, body, buttonLink, buttonText, footnote);
    }

    private static String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
