package com.washflow.data.protocols.messaging;

import com.washflow.domain.errors.auth.OtpUnavailableError;

/**
 * Delivers a one-time code (and its magic link) through one channel: a phone by WhatsApp or an
 * e-mail address.
 */
public interface OtpSender {

  /** Throws {@link OtpUnavailableError} when the channel is off or the delivery failed. */
  void send(String address, String code, String magicLink) throws OtpUnavailableError;

  boolean available();
}
