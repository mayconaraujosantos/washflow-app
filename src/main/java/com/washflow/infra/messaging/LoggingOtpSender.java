package com.washflow.infra.messaging;

import com.washflow.data.protocols.messaging.OtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Development stand-in for a WhatsApp provider: writes the code and magic link to the server log.
 * Never enabled in production (see AccessFactory).
 */
public class LoggingOtpSender implements OtpSender {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingOtpSender.class);

  @Override
  public void send(String phone, String code, String magicLink) {
    LOGGER.info("[DEV] WhatsApp code for {}: {} - link: {}", phone, code, magicLink);
  }

  @Override
  public boolean available() {
    return true;
  }
}
