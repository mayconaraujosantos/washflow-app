package com.washflow.application.config;

/**
 * SMTP relay of the e-mail channel: Brevo, Resend, Amazon SES, Gmail app password; Mailpit locally.
 *
 * @param security "ssl" (implicit TLS, port 465), "starttls" (587) or "none" (local catcher)
 */
public record SmtpConfig(
    String host,
    int port,
    String username,
    String password,
    String security,
    String from,
    String fromName) {

  public enum Security {
    SSL,
    STARTTLS,
    NONE
  }

  public static SmtpConfig from(EnvSource env) {
    Security security =
        Choices.parse(
            Security.class, env.getOrDefault("SMTP_SECURITY", "starttls"), "SMTP_SECURITY");
    return new SmtpConfig(
        env.getOrDefault("SMTP_HOST", ""),
        Ports.parse(env.getOrDefault("SMTP_PORT", "587"), "SMTP_PORT"),
        env.getOrDefault("SMTP_USERNAME", ""),
        env.getOrDefault("SMTP_PASSWORD", ""),
        Choices.lower(security),
        env.getOrDefault("SMTP_FROM", ""),
        env.getOrDefault("SMTP_FROM_NAME", "Washflow"));
  }

  @Override
  public String toString() {
    return "Smtp[host="
        + host
        + ", port="
        + port
        + ", security="
        + security
        + ", from="
        + from
        + ", password=[redacted]]";
  }
}
