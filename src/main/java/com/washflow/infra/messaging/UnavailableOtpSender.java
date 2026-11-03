package com.washflow.infra.messaging;

import com.washflow.data.protocols.messaging.OtpSender;
import com.washflow.domain.errors.OtpUnavailableError;

/** No WhatsApp provider configured: the option is hidden and requests are refused. */
public class UnavailableOtpSender implements OtpSender {

  @Override
  public void send(String phone, String code, String magicLink) throws OtpUnavailableError {
    throw new OtpUnavailableError();
  }

  @Override
  public boolean available() {
    return false;
  }
}
