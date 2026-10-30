package com.washflow.data.usecases.authenticateuser;

import com.washflow.application.AppConfig;
import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.db.CreateUserRepository;
import com.washflow.data.protocols.db.LoadUserByPhoneRepository;
import com.washflow.data.protocols.db.LoadUserPasswordHashRepository;
import com.washflow.data.protocols.db.SaveUserPasswordHashRepository;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.InvalidCredentialsError;
import com.washflow.domain.errors.ManagerSelfRegistrationNotAllowedError;
import com.washflow.domain.errors.UserProfileMismatchError;
import com.washflow.domain.errors.WasherSelfRegistrationNotAllowedError;
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
  private final LoadUserPasswordHashRepository loadUserPasswordHashRepository;
  private final SaveUserPasswordHashRepository saveUserPasswordHashRepository;
  private final PasswordHasher passwordHasher;
  private final AppConfig appConfig;

  public DbAuthenticateUser(
      LoadUserByPhoneRepository loadUserByPhoneRepository,
      CreateUserRepository createUserRepository,
      LoadUserPasswordHashRepository loadUserPasswordHashRepository,
      SaveUserPasswordHashRepository saveUserPasswordHashRepository,
      PasswordHasher passwordHasher,
      AppConfig appConfig) {
    this.loadUserByPhoneRepository = loadUserByPhoneRepository;
    this.createUserRepository = createUserRepository;
    this.loadUserPasswordHashRepository = loadUserPasswordHashRepository;
    this.saveUserPasswordHashRepository = saveUserPasswordHashRepository;
    this.passwordHasher = passwordHasher;
    this.appConfig = appConfig;
  }

  @Override
  public User authenticate(Params params)
      throws UserProfileMismatchError,
          ManagerSelfRegistrationNotAllowedError,
          WasherSelfRegistrationNotAllowedError,
          InvalidCredentialsError {
    var existingUser = loadUserByPhoneRepository.loadByPhone(params.phone());

    if (existingUser.isPresent()) {
      User user = existingUser.get();
      if (!user.active()) {
        throw new InvalidCredentialsError();
      }
      if (user.profile() != params.expectedProfile()) {
        throw new UserProfileMismatchError(
            params.phone(), user.profile(), params.expectedProfile());
      }
      var passwordHash = loadUserPasswordHashRepository.loadPasswordHash(user.id());
      if (passwordHash.isPresent()) {
        if (!passwordHasher.matches(params.password(), passwordHash.get())) {
          throw new InvalidCredentialsError();
        }
      } else if (appConfig.canClaimUnconfiguredUsers()
          || isBootstrapManager(user, params.password())) {
        saveUserPasswordHashRepository.savePasswordHash(
            user.id(), passwordHasher.hash(params.password()));
      } else {
        throw new InvalidCredentialsError();
      }
      return user;
    }

    if (params.expectedProfile() != UserProfile.CUSTOMER) {
      if (params.expectedProfile() == UserProfile.MANAGER) {
        throw new ManagerSelfRegistrationNotAllowedError(params.phone());
      }
      throw new WasherSelfRegistrationNotAllowedError(params.phone());
    }

    return createUserRepository.create(
        new User(
            UUID.randomUUID(),
            params.name(),
            params.phone(),
            params.expectedProfile(),
            Instant.now()),
        passwordHasher.hash(params.password()));
  }

  private boolean isBootstrapManager(User user, String password) {
    return user.profile() == UserProfile.MANAGER
        && user.phone().equals(appConfig.bootstrapManagerPhone())
        && !appConfig.bootstrapManagerPassword().isBlank()
        && appConfig.bootstrapManagerPassword().equals(password);
  }
}
