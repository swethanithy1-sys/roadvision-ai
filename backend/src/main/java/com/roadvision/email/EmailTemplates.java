package com.roadvision.email;

/** Branded HTML for the one-time-code emails, styled to match the app's theme tokens. */
final class EmailTemplates {

    private EmailTemplates() {
    }

    static String verificationCodeHtml(String name, String code) {
        return layout(
                "Verify your email",
                "Hi " + escape(name) + ",",
                "Thanks for signing up for RoadVision AI. Enter this code in the app to confirm "
                        + "your email address and activate your account.",
                code,
                "This code expires shortly. If you didn't create this account, you can safely "
                        + "ignore this email."
        );
    }

    static String passwordResetCodeHtml(String name, String code) {
        return layout(
                "Reset your password",
                "Hi " + escape(name) + ",",
                "We received a request to reset your RoadVision AI password. Enter this code in "
                        + "the app to choose a new one.",
                code,
                "This code expires shortly. If you didn't request this, you can safely ignore "
                        + "this email — your password won't change."
        );
    }

    private static String layout(String title, String greeting, String body, String code, String footnote) {
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
                              <table role="presentation" cellpadding="0" cellspacing="0" style="width:100%%;">
                                <tr>
                                  <td align="center" style="background-color:#F8FAFC;border:1px solid #E2E8F0;border-radius:8px;padding:20px;">
                                    <span style="font-size:32px;font-weight:700;letter-spacing:0.3em;color:#2563EB;">%s</span>
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
                """.formatted(title, greeting, body, code, footnote);
    }

    private static String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
