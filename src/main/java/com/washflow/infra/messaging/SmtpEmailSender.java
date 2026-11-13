package com.washflow.infra.messaging;

import com.washflow.application.config.SmtpConfig;
import com.washflow.data.protocols.messaging.EmailSendError;
import com.washflow.data.protocols.messaging.EmailSender;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

/**
 * The one SMTP transport of the app (access codes and password-reset codes), configured by the
 * {@code SMTP_*} variables: Mailpit locally; Brevo, Resend, Amazon SES, Gmail... in production.
 */
public class SmtpEmailSender implements EmailSender {

  private static final String TIMEOUT_MILLIS = "10000";

  private final Session session;
  private final InternetAddress from;

  public SmtpEmailSender(SmtpConfig smtp) {
    boolean auth = !smtp.username().isBlank();
    Properties props = properties(smtp, Security.parse(smtp.security()), auth);
    this.session =
        auth
            ? Session.getInstance(
                props,
                new Authenticator() {
                  @Override
                  protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtp.username(), smtp.password());
                  }
                })
            : Session.getInstance(props);
    try {
      this.from = new InternetAddress(smtp.from(), true);
      if (!smtp.fromName().isBlank()) {
        this.from.setPersonal(smtp.fromName(), StandardCharsets.UTF_8.name());
      }
    } catch (Exception e) {
      throw new IllegalStateException("SMTP_FROM is not a valid address", e);
    }
  }

  @Override
  public void send(String to, String subject, String body) {
    try {
      MimeMessage message = new MimeMessage(session);
      message.setFrom(from);
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, true));
      message.setSubject(subject, StandardCharsets.UTF_8.name());
      message.setText(body, StandardCharsets.UTF_8.name());
      message.setSentDate(new Date());
      Transport.send(message);
    } catch (MessagingException e) {
      throw new EmailSendError("Could not send the e-mail", e);
    }
  }

  @Override
  public boolean available() {
    return true;
  }

  private static Properties properties(SmtpConfig smtp, Security security, boolean auth) {
    Properties props = new Properties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.host", smtp.host());
    props.put("mail.smtp.port", String.valueOf(smtp.port()));
    props.put("mail.smtp.auth", String.valueOf(auth));
    props.put("mail.smtp.connectiontimeout", TIMEOUT_MILLIS);
    props.put("mail.smtp.timeout", TIMEOUT_MILLIS);
    props.put("mail.smtp.writetimeout", TIMEOUT_MILLIS);
    switch (security) {
      case SSL -> {
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.checkserveridentity", "true");
      }
      case STARTTLS -> {
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.checkserveridentity", "true");
      }
      case NONE -> {
        // Plain SMTP: only for a local catcher such as Mailpit.
      }
    }
    return props;
  }

  /** How the connection is protected: implicit TLS (465), STARTTLS (587) or none (Mailpit). */
  public enum Security {
    SSL,
    STARTTLS,
    NONE;

    public static Security parse(String value) {
      try {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException | NullPointerException e) {
        throw new IllegalArgumentException("SMTP_SECURITY deve ser ssl, starttls ou none", e);
      }
    }
  }
}
