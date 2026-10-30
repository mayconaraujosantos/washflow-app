package com.washflow.data.usecases.createwasher;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.db.CreateWasherRepository;
import com.washflow.data.protocols.db.LoadUserByPhoneRepository;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.PhoneAlreadyRegisteredError;
import com.washflow.domain.usecases.CreateWasher;
import java.time.Instant;
import java.util.UUID;

/**
 * Validates washer creation params, checks for duplicate phone, hashes the password, and delegates
 * to {@code CreateWasherRepository}. Depends only on port interfaces, never on JDBI or Postgres.
 */
public class DbCreateWasher implements CreateWasher {

  private final CreateWasherRepository createRepository;
  private final LoadUserByPhoneRepository loadByPhoneRepository;
  private final PasswordHasher passwordHasher;

  public DbCreateWasher(
      CreateWasherRepository createRepository,
      LoadUserByPhoneRepository loadByPhoneRepository,
      PasswordHasher passwordHasher) {
    this.createRepository = createRepository;
    this.loadByPhoneRepository = loadByPhoneRepository;
    this.passwordHasher = passwordHasher;
  }

  @Override
  public User create(Params params) throws PhoneAlreadyRegisteredError {
    String name = params.name() == null ? "" : params.name().trim();
    String phone = params.phone() == null ? "" : params.phone().trim();
    String password = params.password() == null ? "" : params.password();

    if (name.isBlank() || name.length() > 100 || phone.isBlank() || phone.length() > 20) {
      throw new IllegalArgumentException("Name and phone are required and must be valid");
    }
    if (password.length() < 8) {
      throw new IllegalArgumentException("Password must contain at least eight characters");
    }

    if (loadByPhoneRepository.loadByPhone(phone).isPresent()) {
      throw new PhoneAlreadyRegisteredError();
    }

    User washer = new User(UUID.randomUUID(), name, phone, UserProfile.WASHER, Instant.now(), true);
    String passwordHash = passwordHasher.hash(password);

    return createRepository.createWasher(washer, passwordHash);
  }
}
