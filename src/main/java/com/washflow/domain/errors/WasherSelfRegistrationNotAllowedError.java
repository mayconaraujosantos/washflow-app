package com.washflow.domain.errors;

public class WasherSelfRegistrationNotAllowedError extends Exception {

  public WasherSelfRegistrationNotAllowedError(String phone) {
    super("Phone " + phone + " is not registered as a washer yet; ask a manager to promote it");
  }
}
