package com.washflow.data.usecases.establishment;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.db.EstablishmentRepository;
import com.washflow.data.protocols.db.LoadUserByEmailRepository;
import com.washflow.data.protocols.db.LoadUserByPhoneRepository;
import com.washflow.data.usecases.shared.ContactRules;
import com.washflow.domain.entities.Establishment;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.EmailAlreadyRegisteredError;
import com.washflow.domain.errors.EstablishmentAlreadyConfiguredError;
import com.washflow.domain.errors.InvalidSetupCodeError;
import com.washflow.domain.errors.PhoneAlreadyRegisteredError;
import com.washflow.domain.usecases.SetupEstablishment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code setupCode} is the server-side {@code SETUP_CODE}; when {@code setupCodeRequired} (always
 * in production) a deployment without it cannot be configured through the API at all, so a fresh
 * production install is never claimable by whoever finds the URL first.
 */
public class DbSetupEstablishment implements SetupEstablishment {

  private final EstablishmentRepository establishmentRepository;
  private final LoadUserByEmailRepository loadUserByEmailRepository;
  private final LoadUserByPhoneRepository loadUserByPhoneRepository;
  private final PasswordHasher passwordHasher;
  private final String setupCode;
  private final boolean setupCodeRequired;
  private final Clock clock;

  @SuppressWarnings("java:S107") // one port per collaborator, like the other Db* use cases
  public DbSetupEstablishment(
      EstablishmentRepository establishmentRepository,
      LoadUserByEmailRepository loadUserByEmailRepository,
      LoadUserByPhoneRepository loadUserByPhoneRepository,
      PasswordHasher passwordHasher,
      String setupCode,
      boolean setupCodeRequired,
      Clock clock) {
    this.establishmentRepository = establishmentRepository;
    this.loadUserByEmailRepository = loadUserByEmailRepository;
    this.loadUserByPhoneRepository = loadUserByPhoneRepository;
    this.passwordHasher = passwordHasher;
    this.setupCode = setupCode == null ? "" : setupCode;
    this.setupCodeRequired = setupCodeRequired;
    this.clock = clock;
  }

  @Override
  public Status status() {
    Establishment establishment = establishmentRepository.load();
    return new Status(establishment.isConfigured(), setupCodeRequired, establishment.name());
  }

  @Override
  public User setup(Params params)
      throws EstablishmentAlreadyConfiguredError,
          InvalidSetupCodeError,
          EmailAlreadyRegisteredError,
          PhoneAlreadyRegisteredError {
    if (establishmentRepository.load().isConfigured()) {
      throw new EstablishmentAlreadyConfiguredError();
    }
    checkSetupCode(params.setupCode());

    String name = EstablishmentRules.name(params.establishmentName());
    EstablishmentRules.hours(params.openingHour(), params.closingHour());
    EstablishmentRules.capacity(params.maxVehiclesPerHour());
    String ownerName = ContactRules.limited(params.ownerName(), 100, "Seu nome");
    if (ownerName == null) {
      throw new IllegalArgumentException("Informe seu nome");
    }
    String email = ContactRules.normalizeEmail(params.ownerEmail());
    if (email == null) {
      throw new IllegalArgumentException("Informe o e-mail de acesso");
    }
    String phone = ContactRules.normalizePhone(params.ownerPhone());
    if (params.ownerPassword() == null || params.ownerPassword().length() < 8) {
      throw new IllegalArgumentException("Senha deve ter no mínimo 8 caracteres");
    }
    if (loadUserByEmailRepository.loadByEmail(email).isPresent()) {
      throw new EmailAlreadyRegisteredError();
    }
    if (phone != null && loadUserByPhoneRepository.loadByPhone(phone).isPresent()) {
      throw new PhoneAlreadyRegisteredError();
    }

    Instant now = Instant.now(clock);
    return establishmentRepository.completeSetup(
        new Establishment(
            name,
            params.openingHour(),
            params.closingHour(),
            params.openOnSunday(),
            params.maxVehiclesPerHour(),
            now),
        new User(UUID.randomUUID(), ownerName, phone, UserProfile.MANAGER, now, true, email),
        passwordHasher.hash(params.ownerPassword()));
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
