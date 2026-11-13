package com.washflow.domain.errors.whatsapp;

import com.washflow.domain.errors.common.UnavailableError;

/** The server has no WhatsApp provider, or the provider can't be reached right now. */
public class WhatsappUnavailableError extends UnavailableError {

  public WhatsappUnavailableError() {
    this("WhatsApp não está configurado neste servidor");
  }

  public WhatsappUnavailableError(String message) {
    super("WHATSAPP_UNAVAILABLE", message);
  }
}
