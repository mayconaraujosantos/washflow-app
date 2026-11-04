package com.washflow.data.usecases.establishment;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.db.PlatformRepository;
import com.washflow.data.usecases.shared.ContactRules;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.EstablishmentAlreadyConfiguredError;
import com.washflow.domain.errors.InvalidSetupCodeError;
import com.washflow.domain.usecases.SetupEstablishment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * First run of the platform: creates the administrator account (name, e-mail, password), who then
 * creates each lava-jato from the admin panel. Runs once; in production it also needs the server's
 * {@code SETUP_CODE} - without one the wizard stays disabled.
 */
public class DbSetupEstablishment implements SetupEstablishment {

  private final PlatformRepository platformRepository;
  private final PasswordHasher passwordHasher;
  private final String setupCode;
  private final boolean setupCodeRequired;
  private final Clock clock;

  public DbSetupEstablishment(
      PlatformRepository platformRepository,
      PasswordHasher passwordHasher,
      String setupCode,
      boolean setupCodeRequired,
      Clock clock) {
    this.platformRepository = platformRepository;
    this.passwordHasher = passwordHasher;
    this.setupCode = setupCode == null ? "" : setupCode;
    this.setupCodeRequired = setupCodeRequired;
    this.clock = clock;
  }

  @Override
  public Status status() {
    return new Status(platformRepository.adminExists(), setupCodeRequired, null);
  }

  @Override
  public User setup(Params params)
      throws EstablishmentAlreadyConfiguredError, InvalidSetupCodeError {
    if (platformRepository.adminExists()) {
      throw new EstablishmentAlreadyConfiguredError();
    }
    checkSetupCode(params.setupCode());
    String name = ContactRules.limited(params.ownerName(), 100, "Seu nome");
    if (name == null) {
      throw new IllegalArgumentException("Informe seu nome");
    }
    String email = ContactRules.normalizeEmail(params.ownerEmail());
    if (email == null) {
      throw new IllegalArgumentException("Informe o e-mail de acesso");
    }
    if (params.ownerPassword() == null || params.ownerPassword().length() < 8) {
      throw new IllegalArgumentException("Senha deve ter no mínimo 8 caracteres");
    }
    User admin =
        new User(UUID.randomUUID(), name, null, UserProfile.ADMIN, Instant.now(clock), true, email);
    // Guard against two wizards racing: only the first insert wins.
    if (!platformRepository.createFirstAdmin(admin, passwordHasher.hash(params.ownerPassword()))) {
      throw new EstablishmentAlreadyConfiguredError();
    }
    return admin;
  }

  private void checkSetupCode(String provided) throws InvalidSetupCodeError {
    if (!setupCodeRequired) {
      return;
    }
    // A required-but-unset code means setup is disabled on this server.
    if (setupCode.isBlank()
        || provided == null
        || !MessageDigest.isEqual(
            setupCode.getBytes(StandardCharsets.UTF_8),
            provided.trim().getBytes(StandardCharsets.UTF_8))) {
      throw new InvalidSetupCodeError();
    }
  }
}
