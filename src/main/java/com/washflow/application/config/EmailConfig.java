package com.washflow.application.config;

/**
 * The app's single e-mail channel (EMAIL_CHANNEL), shared by the customer access code and the
 * password-reset codes: "smtp", "log" (development only) or "none". Unset, it is "smtp" as soon as
 * SMTP_HOST is, otherwise "log" in development and "none" in production.
 */
public record EmailConfig(Channel channel, SmtpConfig smtp) {

  public enum Channel {
    SMTP,
    LOG,
    NONE
  }

  public static EmailConfig from(EnvSource env, boolean production) {
    SmtpConfig smtp = SmtpConfig.from(env);
    Channel channel =
        env.get("EMAIL_CHANNEL")
            .map(raw -> Choices.parse(Channel.class, raw, "EMAIL_CHANNEL"))
            .orElseGet(() -> defaultChannel(smtp, production));
    if (channel == Channel.SMTP && (smtp.host().isBlank() || smtp.from().isBlank())) {
      throw new ConfigError("EMAIL_CHANNEL=smtp requires SMTP_HOST and SMTP_FROM");
    }
    return new EmailConfig(channel, smtp);
  }

  private static Channel defaultChannel(SmtpConfig smtp, boolean production) {
    if (!smtp.host().isBlank()) {
      return Channel.SMTP;
    }
    return production ? Channel.NONE : Channel.LOG;
  }
}
