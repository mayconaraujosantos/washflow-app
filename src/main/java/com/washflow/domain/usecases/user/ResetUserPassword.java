package com.washflow.domain.usecases.user;

import com.washflow.domain.entities.user.User;
import com.washflow.domain.errors.user.PasswordResetNotAllowedError;
import com.washflow.domain.errors.user.UserNotFoundError;
import java.util.UUID;

/**
 * A manager sets a new password for a customer or washer of the lava-jato who lost theirs; the
 * manager hands it over in person and the person signs in with it.
 */
public interface ResetUserPassword {

  User reset(UUID userId, String newPassword)
      throws UserNotFoundError, PasswordResetNotAllowedError;
}
