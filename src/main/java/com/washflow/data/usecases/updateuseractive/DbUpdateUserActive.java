package com.washflow.data.usecases.updateuseractive;

import com.washflow.data.protocols.db.UpdateUserActiveRepository;
import com.washflow.domain.entities.User;
import com.washflow.domain.errors.UserHasActiveWashesError;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.usecases.UpdateUserActive;

/**
 * Delegates to {@code UpdateUserActiveRepository}. Depends only on the port interface, never on
 * JDBI or Postgres.
 */
public class DbUpdateUserActive implements UpdateUserActive {

  private final UpdateUserActiveRepository updateRepository;

  public DbUpdateUserActive(UpdateUserActiveRepository updateRepository) {
    this.updateRepository = updateRepository;
  }

  @Override
  public User update(Params params) throws UserNotFoundError {
    try {
      return updateRepository.updateActive(params.userId(), params.active());
    } catch (UserHasActiveWashesError e) {
      throw e;
    } catch (Exception e) {
      if (e.getMessage() != null && e.getMessage().contains("not found")) {
        throw new UserNotFoundError(params.userId());
      }
      throw e;
    }
  }
}
