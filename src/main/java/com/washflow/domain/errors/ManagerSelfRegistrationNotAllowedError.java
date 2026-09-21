package com.washflow.domain.errors;

/**
 * A phone number not yet registered tried to log in through the GERENTE QR code. {@code
 * AuthenticateUser} auto-registers first-time CLIENTE/LAVADOR access, but never GERENTE - otherwise
 * anyone who found the manager QR code's URL could self-promote to the most privileged profile. A
 * new manager can only come from an existing one calling {@code UpdateUserProfile}, or from the
 * initial seed data.
 */
public class ManagerSelfRegistrationNotAllowedError extends Exception {

  public ManagerSelfRegistrationNotAllowedError(String phone) {
    super(
        "Phone "
            + phone
            + " is not registered as a manager yet; ask an existing manager to promote it");
  }
}
