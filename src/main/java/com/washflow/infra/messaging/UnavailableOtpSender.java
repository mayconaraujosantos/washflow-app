package com.washflow.infra.messaging;

import com.washflow.data.protocols.messaging.OtpSender;
import com.washflow.domain.errors.auth.OtpUnavailableError;

/** No provider configured for the channel: the option is hidden and requests are refused. */
public class UnavailableOtpSender implements OtpSender {

  private final String message;

  public UnavailableOtpSender() {
    this(new OtpUnavailableError().getMessage());
  }

  public UnavailableOtpSender(String message) {
    this.message = message;
  }

  @Override
  public void send(String address, String code, String magicLink) {
    throw new OtpUnavailableError(message);
  }

  @Override
  public boolean available() {
    return false;
  }
}
