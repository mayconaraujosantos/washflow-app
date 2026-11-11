package com.washflow.infra.messaging;

import com.washflow.data.protocols.messaging.OtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development stand-in for a real provider (WhatsApp or e-mail): writes the code and magic link to
 * the server log. Never enabled in production (see CustomerModule).
 */
public class LoggingOtpSender implements OtpSender {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingOtpSender.class);

  private final String channel;

  public LoggingOtpSender() {
    this("WhatsApp");
  }

  public LoggingOtpSender(String channel) {
    this.channel = channel;
  }

  @Override
  public void send(String address, String code, String magicLink) {
    LOGGER.info("[DEV] {} code for {}: {} - link: {}", channel, address, code, magicLink);
  }

  @Override
  public boolean available() {
    return true;
  }
}
