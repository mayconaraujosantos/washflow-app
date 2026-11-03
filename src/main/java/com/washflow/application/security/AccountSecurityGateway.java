package com.washflow.application.security;

import com.washflow.domain.entities.UserProfile;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstraction isolating the database query that checks whether a user's account is active and
 * whether the stored profile matches the token claim.
 */
public interface AccountSecurityGateway {

  /** Returns the account access record for the given user id, if the user exists. */
  Optional<AccountAccess> findAccessById(UUID userId);

  /** Record holding the stored profile and active flag of a user account. */
  record AccountAccess(UserProfile profile, boolean active) {}
}
