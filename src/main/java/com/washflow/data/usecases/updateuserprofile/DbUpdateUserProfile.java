package com.washflow.data.usecases.updateuserprofile;

import com.washflow.data.protocols.db.LoadUserByIdRepository;
import com.washflow.data.protocols.db.UpdateUserProfileRepository;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.ForbiddenError;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.usecases.UpdateUserProfile;

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
  public User update(Params params) throws UserNotFoundError, ForbiddenError {
    User requestedBy =
        loadUserByIdRepository
            .loadById(params.requestedByUserId())
            .orElseThrow(() -> new UserNotFoundError(params.requestedByUserId()));

    if (requestedBy.profile() != UserProfile.MANAGER) {
      throw new ForbiddenError("Only a manager can update another user's profile");
    }

    loadUserByIdRepository
        .loadById(params.targetUserId())
        .orElseThrow(() -> new UserNotFoundError(params.targetUserId()));

    return updateUserProfileRepository.updateProfile(params.targetUserId(), params.newProfile());
  }
}
