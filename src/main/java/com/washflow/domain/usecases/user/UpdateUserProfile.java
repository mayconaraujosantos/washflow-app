package com.washflow.domain.usecases.user;

import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.common.ForbiddenError;
import java.util.UUID;

/**
 * Lets a GERENTE change any account's profile, including their own - e.g. a LAVADOR later promoted
 * to GERENTE. {@code requestedByUserId} must resolve to a user whose profile is {@link
 * UserProfile#MANAGER}, or the request is rejected with {@link ForbiddenError}; without this check
 * any caller could hand themselves the GERENTE profile through this same endpoint.
 *
 * <p>Implemented by {@code DbUpdateUserProfile} in the data layer.
 */
public interface UpdateUserProfile {

  User update(Params params) throws ForbiddenError;

  record Params(UUID requestedByUserId, UUID targetUserId, UserProfile newProfile) {}
}
