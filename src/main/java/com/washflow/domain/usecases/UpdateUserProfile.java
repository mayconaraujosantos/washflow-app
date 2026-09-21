package com.washflow.domain.usecases;

import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.ForbiddenError;
import com.washflow.domain.errors.UserNotFoundError;
import java.util.UUID;

/**
 * Lets a GERENTE correct or promote another user's profile - e.g. a LAVADOR later promoted to
 * GERENTE. {@code requestedByUserId} must resolve to a user whose profile is {@link
 * UserProfile#MANAGER}, or the request is rejected with {@link ForbiddenError}; without this check
 * any caller could hand themselves the GERENTE profile through this same endpoint.
 *
 * <p>Implemented by {@code DbUpdateUserProfile} in the data layer.
 */
public interface UpdateUserProfile {

  User update(Params params) throws UserNotFoundError, ForbiddenError;

  record Params(UUID requestedByUserId, UUID targetUserId, UserProfile newProfile) {}
}
