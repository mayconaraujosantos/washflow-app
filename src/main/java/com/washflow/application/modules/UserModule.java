package com.washflow.application.modules;

import com.washflow.application.AppConfig;
import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.usecases.user.DbAuthenticateUser;
import com.washflow.data.usecases.user.DbCreateWasher;
import com.washflow.data.usecases.user.DbListUsers;
import com.washflow.data.usecases.user.DbUpdateUserActive;
import com.washflow.data.usecases.user.DbUpdateUserProfile;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.usecases.user.AuthenticateUser;
import com.washflow.domain.usecases.user.CreateWasher;
import com.washflow.domain.usecases.user.ListUsers;
import com.washflow.domain.usecases.user.UpdateUserActive;
import com.washflow.domain.usecases.user.UpdateUserProfile;
import com.washflow.infra.db.jdbi.user.UserJdbiRepository;
import com.washflow.infra.security.BCryptPasswordHasher;
import com.washflow.presentation.controllers.user.AuthenticateUserController;
import com.washflow.presentation.controllers.user.CreateWasherController;
import com.washflow.presentation.controllers.user.ListUsersController;
import com.washflow.presentation.controllers.user.UpdateUserActiveController;
import com.washflow.presentation.controllers.user.UpdateUserProfileController;
import com.washflow.presentation.protocols.Controller;
import com.washflow.validation.protocols.Validation;
import com.washflow.validation.validators.AnyRequiredFieldValidation;
import com.washflow.validation.validators.RequiredFieldValidation;
import com.washflow.validation.validators.ValidationComposite;
import java.util.List;
import org.jdbi.v3.core.Jdbi;

/** Composition root of accounts, sign-in and the team: builds its use cases and controllers. */
public final class UserModule {

  private static final String PHONE = "phone";
  private static final String PASSWORD = "password";

  private UserModule() {}

  /**
   * Swapping JDBI for Hibernate later means changing only the {@code new UserJdbiRepository(...)}
   * call here - {@code DbAuthenticateUser} depends on the {@code data.protocols.db} interfaces, not
   * on this concrete class.
   */
  public static AuthenticateUser authenticateUser(Jdbi jdbi, AppConfig appConfig) {
    UserJdbiRepository userRepository = new UserJdbiRepository(jdbi);
    return new DbAuthenticateUser(
        userRepository,
        userRepository,
        userRepository,
        userRepository,
        new BCryptPasswordHasher(),
        appConfig,
        userRepository);
  }

  public static CreateWasher createWasher(Jdbi jdbi) {
    UserJdbiRepository repository = new UserJdbiRepository(jdbi);
    PasswordHasher passwordHasher = new BCryptPasswordHasher();
    return new DbCreateWasher(repository, repository, passwordHasher);
  }

  public static ListUsers listUsers(Jdbi jdbi) {
    UserJdbiRepository repository = new UserJdbiRepository(jdbi);
    return new DbListUsers(repository);
  }

  public static UpdateUserActive updateUserActive(Jdbi jdbi) {
    UserJdbiRepository repository = new UserJdbiRepository(jdbi);
    return new DbUpdateUserActive(repository);
  }

  /**
   * Swapping JDBI for Hibernate later means changing only the {@code new UserJdbiRepository(...)}
   * call here - {@code DbUpdateUserProfile} depends on the {@code data.protocols.db} interfaces,
   * not on this concrete class.
   */
  public static UpdateUserProfile updateUserProfile(Jdbi jdbi) {
    UserJdbiRepository userRepository = new UserJdbiRepository(jdbi);
    return new DbUpdateUserProfile(userRepository, userRepository);
  }

  /**
   * Backs the CLIENTE QR code - {@link UserProfile#CUSTOMER} is fixed here, never client-supplied.
   */
  public static Controller authenticateCustomerController(Jdbi jdbi, AppConfig appConfig) {
    Validation validation =
        new ValidationComposite(
            List.of(
                // "name" is only needed to create the account - DbAuthenticateUser checks it
                // there, so returning customers can sign in with phone and password.
                new RequiredFieldValidation(PHONE), new RequiredFieldValidation(PASSWORD)));

    return new AuthenticateUserController(
        authenticateUser(jdbi, appConfig), validation, UserProfile.CUSTOMER);
  }

  /**
   * Backs the GERENTE QR code - {@link UserProfile#MANAGER} is fixed here, never client-supplied.
   */
  public static Controller authenticateManagerController(Jdbi jdbi, AppConfig appConfig) {
    Validation validation =
        new ValidationComposite(
            List.of(
                new AnyRequiredFieldValidation("email", PHONE),
                new RequiredFieldValidation(PASSWORD)));

    return new AuthenticateUserController(
        authenticateUser(jdbi, appConfig), validation, UserProfile.MANAGER);
  }

  /**
   * Backs the LAVADOR QR code - {@link UserProfile#WASHER} is fixed here, never client-supplied.
   */
  public static Controller authenticateWasherController(Jdbi jdbi, AppConfig appConfig) {
    Validation validation =
        new ValidationComposite(
            List.of(new RequiredFieldValidation(PHONE), new RequiredFieldValidation(PASSWORD)));

    return new AuthenticateUserController(
        authenticateUser(jdbi, appConfig), validation, UserProfile.WASHER);
  }

  public static Controller createWasherController(Jdbi jdbi) {
    return new CreateWasherController(createWasher(jdbi));
  }

  public static Controller listUsersController(Jdbi jdbi) {
    return new ListUsersController(listUsers(jdbi));
  }

  public static Controller updateUserActiveController(Jdbi jdbi) {
    return new UpdateUserActiveController(updateUserActive(jdbi));
  }

  public static Controller updateUserProfileController(Jdbi jdbi) {
    Validation validation =
        new ValidationComposite(
            List.of(
                new RequiredFieldValidation("managerId"),
                new RequiredFieldValidation("newProfile")));

    return new UpdateUserProfileController(updateUserProfile(jdbi), validation);
  }
}
