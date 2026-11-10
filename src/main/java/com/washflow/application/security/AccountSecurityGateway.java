package com.washflow.application.security;

import com.washflow.domain.entities.user.UserProfile;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstraction isolating the database query that checks whether a user's account is active and
 * whether the stored profile matches the token claim.
 */
public interface AccountSecurityGateway {

  /** Returns the account access record for the given user id, if the user exists. */
  Optional<AccountAccess> findAccessById(UUID userId);

  /**
   * Active lava-jato with this address ({@code X-Establishment} header of public screens), if any.
   */
  Optional<UUID> findActiveEstablishmentBySlug(String slug);

  /**
   * Stored profile, whether the account (and its lava-jato) is active, and the lava-jato it belongs
   * to - null for the platform administrator.
   */
  record AccountAccess(UserProfile profile, boolean active, UUID establishmentId) {}
}
