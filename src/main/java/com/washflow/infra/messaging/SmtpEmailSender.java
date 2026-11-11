package com.washflow.infra.messaging;

import com.washflow.application.AppConfig;
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
import java.util.Properties;

/** Sends through any SMTP relay configured by SMTP_HOST/PORT/USERNAME/PASSWORD/FROM. */
public class SmtpEmailSender implements EmailSender {

  private static final String TIMEOUT_MS = "10000";

  private final Session session;
  private final InternetAddress from;

  public SmtpEmailSender(AppConfig.Smtp smtp) {
    Properties props = new Properties();
    props.put("mail.smtp.host", smtp.host());
    props.put("mail.smtp.port", String.valueOf(smtp.port()));
    props.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
    props.put("mail.smtp.timeout", TIMEOUT_MS);
    props.put("mail.smtp.writetimeout", TIMEOUT_MS);
    if (smtp.port() == 465) {
      props.put("mail.smtp.ssl.enable", "true");
    } else if (smtp.startTls()) {
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.starttls.required", "true");
    }
    boolean auth = !smtp.username().isBlank();
    props.put("mail.smtp.auth", String.valueOf(auth));
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
    } catch (MessagingException e) {
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
      throw new EmailDeliveryException("Could not send the e-mail", e);
    }
  }

  @Override
  public boolean available() {
    return true;
  }
}
