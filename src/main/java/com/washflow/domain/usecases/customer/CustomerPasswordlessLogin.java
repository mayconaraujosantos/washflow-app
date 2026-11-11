package com.washflow.domain.usecases.customer;

import com.washflow.domain.entities.customer.OtpRecipient;
import com.washflow.domain.entities.user.User;

/**
 * Customers sign in with a one-time code sent by WhatsApp or e-mail (also usable as a magic link)
 * or with Google. A new phone/e-mail/Google account becomes a customer account; staff accounts
 * never sign in this way.
 */
public interface CustomerPasswordlessLogin {

  Options options();

  /** Sends a 6-digit code; returns how long it stays valid, in seconds. */
  int requestCode(OtpRecipient recipient);

  User verifyCode(OtpRecipient recipient, String code, String name);

  User signInWithGoogle(String idToken);

  record Options(boolean whatsappCode, boolean emailCode, String googleClientId) {}
}
