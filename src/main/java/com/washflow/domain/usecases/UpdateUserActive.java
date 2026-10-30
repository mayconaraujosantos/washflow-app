package com.washflow.domain.usecases;

import com.washflow.domain.entities.User;
import com.washflow.domain.errors.UserNotFoundError;
import java.util.UUID;

/**
 * Updates a user's active status. Implemented by {@code DbUpdateUserActive} in the data layer.
 * {@code UserHasActiveWashesError} is an unchecked exception and is not declared in throws.
 */
public interface UpdateUserActive {

  User update(Params params) throws UserNotFoundError;

  record Params(UUID userId, boolean active) {}
}
