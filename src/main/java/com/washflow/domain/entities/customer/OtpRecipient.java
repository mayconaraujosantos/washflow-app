package com.washflow.domain.entities.customer;

import com.washflow.domain.entities.shared.ContactRules;

/** Where a customer's one-time code goes: a phone (by WhatsApp) or an e-mail address. */
public record OtpRecipient(Channel channel, String address) {

  public enum Channel {
    WHATSAPP,
    EMAIL
  }

  /** Normalized phone (digits only); rejects a blank or malformed one. */
  public static OtpRecipient phone(String raw) {
    String phone = ContactRules.normalizePhone(raw);
    if (phone == null) {
      throw new IllegalArgumentException("Informe o telefone com DDD");
    }
    return new OtpRecipient(Channel.WHATSAPP, phone);
  }

  /** Normalized (lower-case) e-mail; rejects a blank or malformed one. */
  public static OtpRecipient email(String raw) {
    String email = ContactRules.normalizeEmail(raw);
    if (email == null) {
      throw new IllegalArgumentException("Informe o e-mail");
    }
    return new OtpRecipient(Channel.EMAIL, email);
  }

  public boolean isEmail() {
    return channel == Channel.EMAIL;
  }
}
