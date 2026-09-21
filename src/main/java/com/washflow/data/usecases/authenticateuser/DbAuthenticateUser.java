package com.washflow.data.usecases.authenticateuser;

import com.washflow.data.protocols.db.CreateUserRepository;
import com.washflow.data.protocols.db.LoadUserByPhoneRepository;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.ManagerSelfRegistrationNotAllowedError;
import com.washflow.domain.errors.UserProfileMismatchError;
import com.washflow.domain.usecases.AuthenticateUser;
import java.time.Instant;
import java.util.UUID;

/**
 * Only talks to {@code data.protocols.db} ports - never to JDBI, Postgres, or anything else
 * concrete.
 */
public class DbAuthenticateUser implements AuthenticateUser {

  private final LoadUserByPhoneRepository loadUserByPhoneRepository;
  private final CreateUserRepository createUserRepository;

  public DbAuthenticateUser(
      LoadUserByPhoneRepository loadUserByPhoneRepository,
      CreateUserRepository createUserRepository) {
    this.loadUserByPhoneRepository = loadUserByPhoneRepository;
    this.createUserRepository = createUserRepository;
  }

  @Override
  public User authenticate(Params params)
      throws UserProfileMismatchError, ManagerSelfRegistrationNotAllowedError {
    var existingUser = loadUserByPhoneRepository.loadByPhone(params.phone());

    if (existingUser.isPresent()) {
      User user = existingUser.get();
      if (user.profile() != params.expectedProfile()) {
        throw new UserProfileMismatchError(
            params.phone(), user.profile(), params.expectedProfile());
      }
      return user;
    }

    if (params.expectedProfile() == UserProfile.MANAGER) {
      throw new ManagerSelfRegistrationNotAllowedError(params.phone());
    }

    return createUserRepository.create(
        new User(
            UUID.randomUUID(),
            params.name(),
            params.phone(),
            params.expectedProfile(),
            Instant.now()));
  }
}
