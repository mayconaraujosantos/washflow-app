package com.washflow.data.usecases.user;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.db.shared.DuplicateKeyError;
import com.washflow.data.protocols.db.user.CreateWasherRepository;
import com.washflow.data.protocols.db.user.LoadUserByPhoneRepository;
import com.washflow.domain.entities.shared.ContactRules;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.user.PhoneAlreadyRegisteredError;
import com.washflow.domain.usecases.user.CreateWasher;
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
  public User create(Params params) {
    String name = params.name() == null ? "" : params.name().trim();
    // Same spelling as every other account ("+55 92 8606-1046" is 92986061046), so the
    // phone of an existing customer or washer is recognized however it was typed.
    String normalized = ContactRules.normalizePhone(params.phone());
    String phone = normalized == null ? "" : normalized;
    String password = params.password() == null ? "" : params.password();

    if (name.isBlank() || name.length() > 100 || phone.isBlank() || phone.length() > 20) {
      throw new IllegalArgumentException("Name and phone are required and must be valid");
    }
    // Optional: washers may sign in with a badge (PIN/QR) issued by the manager instead.
    if (!password.isEmpty() && password.length() < 8) {
      throw new IllegalArgumentException("Password must contain at least eight characters");
    }

    if (loadByPhoneRepository.loadByPhone(phone).isPresent()) {
      throw new PhoneAlreadyRegisteredError();
    }

    User washer = new User(UUID.randomUUID(), name, phone, UserProfile.WASHER, Instant.now(), true);
    String passwordHash = password.isEmpty() ? null : passwordHasher.hash(password);

    try {
      return createRepository.createWasher(washer, passwordHash);
    } catch (DuplicateKeyError e) {
      throw new PhoneAlreadyRegisteredError(); // created by a concurrent request
    }
  }
}
