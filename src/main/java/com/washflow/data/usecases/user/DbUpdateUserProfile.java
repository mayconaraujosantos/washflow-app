package com.washflow.data.usecases.user;

import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.data.protocols.db.user.UpdateUserProfileRepository;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.common.ForbiddenError;
import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.usecases.user.UpdateUserProfile;

/**
 * Only talks to {@code data.protocols.db} ports - never to JDBI, Postgres, or anything else
 * concrete.
 */
public class DbUpdateUserProfile implements UpdateUserProfile {

  private final LoadUserByIdRepository loadUserByIdRepository;
  private final UpdateUserProfileRepository updateUserProfileRepository;

  public DbUpdateUserProfile(
      LoadUserByIdRepository loadUserByIdRepository,
      UpdateUserProfileRepository updateUserProfileRepository) {
    this.loadUserByIdRepository = loadUserByIdRepository;
    this.updateUserProfileRepository = updateUserProfileRepository;
  }

  @Override
  public User update(Params params) throws ForbiddenError {
    User requestedBy =
        loadUserByIdRepository
            .loadById(params.requestedByUserId())
            .orElseThrow(() -> new UserNotFoundError(params.requestedByUserId()));

    if (requestedBy.profile() != UserProfile.MANAGER) {
      throw new ForbiddenError("Only a manager can change account profiles");
    }

    loadUserByIdRepository
        .loadById(params.targetUserId())
        .orElseThrow(() -> new UserNotFoundError(params.targetUserId()));

    return updateUserProfileRepository.updateProfile(params.targetUserId(), params.newProfile());
  }
}
