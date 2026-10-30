package com.washflow.domain.errors;

import java.util.UUID;

public class UserHasActiveWashesError extends RuntimeException {

  public UserHasActiveWashesError(UUID userId) {
    super("Resolve the user's active washes before changing this profile: " + userId);
  }
}
