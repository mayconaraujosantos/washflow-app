package com.washflow.domain.errors.user;

import com.washflow.domain.errors.common.NotFoundError;
import java.util.UUID;

public class UserNotFoundError extends NotFoundError {

  public UserNotFoundError(UUID userId) {
    super("USER_NOT_FOUND", "User not found: " + userId);
  }
}
