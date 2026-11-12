package com.washflow.infra.messaging;

import com.washflow.data.protocols.messaging.EmailSendError;
import com.washflow.data.protocols.messaging.EmailSender;
import com.washflow.data.protocols.messaging.OtpSender;
import com.washflow.domain.errors.auth.OtpUnavailableError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The customer's access code by e-mail: writes the message and hands it to the app's single {@link
 * EmailSender} (SMTP, the development log, or none - see UserModule.emailSender).
 */
public class EmailOtpSender implements OtpSender {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmailOtpSender.class);

  private final EmailSender emailSender;

  public EmailOtpSender(EmailSender emailSender) {
    this.emailSender = emailSender;
  }

  @Override
  public void send(String email, String code, String magicLink) {
    if (!emailSender.available()) {
      throw new OtpUnavailableError("Envio de código por e-mail não está configurado");
    }
    try {
      emailSender.send(email, code + " é o seu código de acesso", body(code, magicLink));
    } catch (EmailSendError e) {
      // The provider's reason goes to the log, never to the customer.
      LOGGER.error("event=otp_email_failed reason={}", e.getMessage(), e);
      throw new OtpUnavailableError("Não foi possível enviar o e-mail agora. Tente de novo.");
    }
  }

  @Override
  public boolean available() {
    return emailSender.available();
  }

  static String body(String code, String magicLink) {
    return "Seu código de acesso é: "
        + code
        + "\n\nOu toque no link para entrar direto:\n"
        + magicLink
        + "\n\nO código vale por 10 minutos. Se não foi você quem pediu, ignore este e-mail.\n";
  }
}
