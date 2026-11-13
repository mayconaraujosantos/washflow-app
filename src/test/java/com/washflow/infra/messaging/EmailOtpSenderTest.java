package com.washflow.infra.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.application.config.SmtpConfig;
import com.washflow.domain.errors.auth.OtpUnavailableError;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;

class EmailOtpSenderTest {

  @Test
  void theBodyHasTheCodeAndTheMagicLink() {
    String body = EmailOtpSender.body("123456", "https://app.test/entrar/email?codigo=123456");

    assertTrue(body.contains("123456"));
    assertTrue(body.contains("https://app.test/entrar/email?codigo=123456"));
  }

  @Test
  void anUnreachableServerIsAFriendlyUnavailableError() throws Exception {
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    }
    EmailOtpSender sender =
        new EmailOtpSender(
            new SmtpEmailSender(
                new SmtpConfig(
                    "127.0.0.1",
                    closedPort,
                    "",
                    "",
                    "none",
                    "nao-responda@washflow.test",
                    "Washflow")));

    OtpUnavailableError error =
        assertThrows(
            OtpUnavailableError.class,
            () -> sender.send("ana@email.com", "123456", "https://app.test"));
    assertEquals("Não foi possível enviar o e-mail agora. Tente de novo.", error.getMessage());
  }

  @Test
  void withoutAChannelTheOptionIsOff() {
    EmailOtpSender sender = new EmailOtpSender(new UnavailableEmailSender());

    assertFalse(sender.available());
    assertThrows(OtpUnavailableError.class, () -> sender.send("ana@email.com", "1", "l"));
  }

  @Test
  void parsesTheSecurityModeAndRejectsAnUnknownOne() {
    assertEquals(SmtpEmailSender.Security.STARTTLS, SmtpEmailSender.Security.parse(" StartTLS"));
    assertEquals(SmtpEmailSender.Security.SSL, SmtpEmailSender.Security.parse("ssl"));
    assertThrows(IllegalArgumentException.class, () -> SmtpEmailSender.Security.parse("tls"));
  }
}
