package com.washflow.domain.errors.user;

import com.washflow.domain.errors.common.AccessDeniedError;

public class WasherSelfRegistrationNotAllowedError extends AccessDeniedError {

  public WasherSelfRegistrationNotAllowedError(String phone) {
    super(
        "WASHER_SELF_REGISTRATION_NOT_ALLOWED",
        "Phone " + phone + " is not registered as a washer yet; ask a manager to promote it");
  }
}
