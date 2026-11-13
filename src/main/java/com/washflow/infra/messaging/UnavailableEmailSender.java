package com.washflow.infra.messaging;

import com.washflow.data.protocols.messaging.EmailSendError;
import com.washflow.data.protocols.messaging.EmailSender;

/** Production without SMTP_HOST: no e-mail channel, so the app hides the e-mail reset. */
public class UnavailableEmailSender implements EmailSender {

  @Override
  public void send(String to, String subject, String body) {
    throw new EmailSendError("E-mail is not configured (SMTP_HOST)", null);
  }

  @Override
  public boolean available() {
    return false;
  }
}
