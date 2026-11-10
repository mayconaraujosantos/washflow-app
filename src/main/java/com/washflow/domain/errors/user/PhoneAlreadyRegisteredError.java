package com.washflow.domain.errors.user;

/** Thrown when a washer creation fails because the phone number is already registered. */
public class PhoneAlreadyRegisteredError extends Exception {

  public PhoneAlreadyRegisteredError() {
    super("Phone is already registered");
  }
}
