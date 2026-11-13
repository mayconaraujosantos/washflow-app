package com.washflow.data.usecases.user;

import com.washflow.application.AppConfig;
import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.db.shared.DuplicateKeyError;
import com.washflow.data.protocols.db.user.CreateUserRepository;
import com.washflow.data.protocols.db.user.LoadUserByEmailRepository;
import com.washflow.data.protocols.db.user.LoadUserByPhoneRepository;
import com.washflow.data.protocols.db.user.LoadUserPasswordHashRepository;
import com.washflow.data.protocols.db.user.SaveUserPasswordHashRepository;
import com.washflow.domain.entities.shared.ContactRules;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.errors.user.ManagerSelfRegistrationNotAllowedError;
import com.washflow.domain.errors.user.UserProfileMismatchError;
import com.washflow.domain.errors.user.WasherSelfRegistrationNotAllowedError;
import com.washflow.domain.usecases.user.AuthenticateUser;
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
  private final LoadUserByEmailRepository loadUserByEmailRepository;

  public DbAuthenticateUser(
      LoadUserByPhoneRepository loadUserByPhoneRepository,
      CreateUserRepository createUserRepository,
      LoadUserPasswordHashRepository loadUserPasswordHashRepository,
      SaveUserPasswordHashRepository saveUserPasswordHashRepository,
      PasswordHasher passwordHasher,
      AppConfig appConfig) {
    this(
        loadUserByPhoneRepository,
        createUserRepository,
        loadUserPasswordHashRepository,
        saveUserPasswordHashRepository,
        passwordHasher,
        appConfig,
        email -> java.util.Optional.empty());
  }

  @SuppressWarnings("java:S107")
  public DbAuthenticateUser(
      LoadUserByPhoneRepository loadUserByPhoneRepository,
      CreateUserRepository createUserRepository,
      LoadUserPasswordHashRepository loadUserPasswordHashRepository,
      SaveUserPasswordHashRepository saveUserPasswordHashRepository,
      PasswordHasher passwordHasher,
      AppConfig appConfig,
      LoadUserByEmailRepository loadUserByEmailRepository) {
    this.loadUserByEmailRepository = loadUserByEmailRepository;
    this.loadUserByPhoneRepository = loadUserByPhoneRepository;
    this.createUserRepository = createUserRepository;
    this.loadUserPasswordHashRepository = loadUserPasswordHashRepository;
    this.saveUserPasswordHashRepository = saveUserPasswordHashRepository;
    this.passwordHasher = passwordHasher;
    this.appConfig = appConfig;
  }

  @Override
  public User authenticate(Params params) {
    if (params.email() != null && !params.email().isBlank()) {
      // E-mail login never self-registers: unknown e-mails are just wrong credentials.
      String email = params.email().trim().toLowerCase(java.util.Locale.ROOT);
      User user =
          loadUserByEmailRepository.loadByEmail(email).orElseThrow(InvalidCredentialsError::new);
      return verifyExisting(user, params);
    }
    // Looked up in the stored spelling (no +55, a mobile with its ninth digit).
    params =
        new Params(
            ContactRules.lookupPhone(params.phone()),
            params.name(),
            params.password(),
            params.expectedProfile(),
            params.email());
    var existingUser = loadUserByPhoneRepository.loadByPhone(params.phone());

    if (existingUser.isPresent()) {
      return verifyExisting(existingUser.get(), params);
    }

    if (params.expectedProfile() != UserProfile.CUSTOMER) {
      if (params.expectedProfile() == UserProfile.MANAGER) {
        throw new ManagerSelfRegistrationNotAllowedError(params.phone());
      }
      throw new WasherSelfRegistrationNotAllowedError(params.phone());
    }

    if (params.name() == null || params.name().isBlank()) {
      // "Entrar" with a phone that has no account in this lava-jato (each one has its own
      // customers): only the sign-up form sends a name.
      throw new IllegalArgumentException(
          "Não encontramos uma conta com este telefone neste lava-jato. Toque em Criar conta"
              + " para se cadastrar.");
    }
    try {
      return createUserRepository.create(
          new User(
              UUID.randomUUID(),
              params.name().trim(),
              ContactRules.normalizePhone(params.phone()),
              params.expectedProfile(),
              Instant.now()),
          passwordHasher.hash(params.password()));
    } catch (DuplicateKeyError e) {
      // The same sign-up sent twice at once (double tap, retry): the other request created the
      // account a moment ago - sign in to it like any existing account (same password checks).
      User created = loadUserByPhoneRepository.loadByPhone(params.phone()).orElseThrow(() -> e);
      return verifyExisting(created, params);
    }
  }

  private User verifyExisting(User user, Params params) {
    if (!user.active()) {
      throw new InvalidCredentialsError();
    }
    if (user.profile() != params.expectedProfile()) {
      throw new UserProfileMismatchError(
          params.phone() != null ? params.phone() : user.phone(),
          user.profile(),
          params.expectedProfile());
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

  private boolean isBootstrapManager(User user, String password) {
    return user.profile() == UserProfile.MANAGER
        && appConfig.bootstrapManagerPhone().equals(user.phone())
        && !appConfig.bootstrapManagerPassword().isBlank()
        && appConfig.bootstrapManagerPassword().equals(password);
  }
}
