package com.washflow.domain.errors;

import com.washflow.domain.entities.UserProfile;

/**
 * The phone number was already claimed by a different profile - e.g. a phone that registered
 * through the LAVADOR QR code trying to log in through the CLIENTE one. The profile is bound to the
 * phone at first access; only {@code UpdateUserProfile} (a manager action) can change it.
 */
public class UserProfileMismatchError extends Exception {

  public UserProfileMismatchError(String phone, UserProfile existing, UserProfile requested) {
    super(
        "Phone "
            + phone
            + " is already registered as "
            + existing
            + ", cannot access as "
            + requested);
  }
}
