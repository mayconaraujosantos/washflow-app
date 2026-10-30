package com.washflow.domain.usecases;

import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.InvalidCredentialsError;
import com.washflow.domain.errors.ManagerSelfRegistrationNotAllowedError;
import com.washflow.domain.errors.UserProfileMismatchError;
import com.washflow.domain.errors.WasherSelfRegistrationNotAllowedError;

/**
 * Login/registration by phone and password, scoped to one profile per QR code (CLIENTE, LAVADOR or
 * GERENTE). {@code expectedProfile} comes from which QR code / route was scanned - never from the
 * request body - so a caller can't self-assign a profile by editing a request field.
 *
 * <p>First access for an unused phone registers a customer. Washer and manager accounts must
 * already exist and can only be provisioned by a manager or initial seed data (see {@link
 * WasherSelfRegistrationNotAllowedError} and {@link ManagerSelfRegistrationNotAllowedError}). Later
 * access with the same phone but a different QR code's profile is rejected - see {@link
 * UserProfileMismatchError}. Only {@code UpdateUserProfile} (a manager action) can change a phone's
 * bound profile afterward. Implemented by {@code DbAuthenticateUser} in the data layer.
 */
public interface AuthenticateUser {

  User authenticate(Params params)
      throws UserProfileMismatchError,
          ManagerSelfRegistrationNotAllowedError,
          WasherSelfRegistrationNotAllowedError,
          InvalidCredentialsError;

  record Params(String phone, String name, String password, UserProfile expectedProfile) {}
}
