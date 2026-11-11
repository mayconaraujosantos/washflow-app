package com.washflow.infra.messaging;

import com.washflow.data.protocols.messaging.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development stand-in without SMTP_HOST: writes the e-mail to the server log. Never used in
 * production (see UserModule), so codes never end up in production logs.
 */
public class LoggingEmailSender implements EmailSender {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingEmailSender.class);

  @Override
  public void send(String to, String subject, String body) {
    LOGGER.info("[DEV] e-mail to {} - {}\n{}", to, subject, body);
  }

  @Override
  public boolean available() {
    return true;
  }
}
