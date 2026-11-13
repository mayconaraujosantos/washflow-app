package com.washflow.domain.errors.user;

import com.washflow.domain.errors.common.ConflictError;

/** Thrown when a washer creation fails because the phone number is already registered. */
public class PhoneAlreadyRegisteredError extends ConflictError {

  public PhoneAlreadyRegisteredError() {
    super("PHONE_ALREADY_REGISTERED", "Phone is already registered");
  }
}
