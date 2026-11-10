package com.washflow.data.protocols.messaging;

import com.washflow.domain.errors.auth.OtpUnavailableError;

/** Delivers a one-time code (and its magic link) to a phone, e.g. by WhatsApp. */
public interface OtpSender {

  void send(String phone, String code, String magicLink) throws OtpUnavailableError;

  boolean available();
}
