package com.washflow.domain.usecases.customer;

import com.washflow.domain.entities.User;
import com.washflow.domain.errors.auth.GoogleLoginUnavailableError;
import com.washflow.domain.errors.auth.InvalidOtpError;
import com.washflow.domain.errors.auth.OtpTooSoonError;
import com.washflow.domain.errors.auth.OtpUnavailableError;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.errors.user.UserProfileMismatchError;

/**
 * Customers sign in with a one-time code sent by WhatsApp (also usable as a magic link) or with
 * Google. A new phone/Google account becomes a customer account; staff accounts never sign in this
 * way.
 */
public interface CustomerPasswordlessLogin {

  Options options();

  /** Sends a 6-digit code; returns how long it stays valid, in seconds. */
  int requestCode(String phone) throws OtpUnavailableError, OtpTooSoonError;

  User verifyCode(String phone, String code, String name)
      throws InvalidOtpError, UserProfileMismatchError;

  User signInWithGoogle(String idToken)
      throws GoogleLoginUnavailableError, InvalidCredentialsError, UserProfileMismatchError;

  record Options(boolean whatsappCode, String googleClientId) {}
}
