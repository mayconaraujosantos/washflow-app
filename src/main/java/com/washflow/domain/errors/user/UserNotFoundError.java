package com.washflow.domain.errors.user;

import java.util.UUID;

public class UserNotFoundError extends Exception {

  public UserNotFoundError(UUID userId) {
    super("User not found: " + userId);
  }
}
