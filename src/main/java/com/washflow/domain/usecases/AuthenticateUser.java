package com.washflow.domain.usecases;

import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.ManagerSelfRegistrationNotAllowedError;
import com.washflow.domain.errors.UserProfileMismatchError;

/**
 * Quick, password-less login by phone number, scoped to one profile per QR code (CLIENTE, LAVADOR
 * or GERENTE). {@code expectedProfile} comes from which QR code / route was scanned - never from
 * the request body - so a caller can't self-assign a profile by editing a request field.
 *
 * <p>First access for a phone auto-registers a new {@code User} with {@code expectedProfile} -
 * except for GERENTE, which never self-registers (see {@link
 * ManagerSelfRegistrationNotAllowedError}); a manager can only be created by an existing one
 * through {@code UpdateUserProfile}, or by the initial seed data. Later access with the same phone
 * but a different QR code's profile is rejected - see {@link UserProfileMismatchError}. Only {@code
 * UpdateUserProfile} (a manager action) can change a phone's bound profile afterward. Implemented
 * by {@code DbAuthenticateUser} in the data layer.
 */
public interface AuthenticateUser {

  User authenticate(Params params)
      throws UserProfileMismatchError, ManagerSelfRegistrationNotAllowedError;

  record Params(String phone, String name, UserProfile expectedProfile) {}
}
