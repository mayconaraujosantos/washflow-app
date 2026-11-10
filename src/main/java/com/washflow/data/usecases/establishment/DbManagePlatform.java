package com.washflow.data.usecases.establishment;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.db.establishment.PlatformRepository;
import com.washflow.data.usecases.shared.ContactRules;
import com.washflow.domain.entities.Establishment;
import com.washflow.domain.entities.EstablishmentListing;
import com.washflow.domain.entities.EstablishmentSummary;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.establishment.EstablishmentNotFoundError;
import com.washflow.domain.errors.establishment.EstablishmentSlugTakenError;
import com.washflow.domain.errors.user.EmailAlreadyRegisteredError;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.usecases.establishment.ManagePlatform;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DbManagePlatform implements ManagePlatform {

  private static final int SEARCH_LIMIT = 20;
  private static final int SLUG_MAX = 40;

  private final PlatformRepository repository;
  private final PasswordHasher passwordHasher;
  private final Clock clock;

  public DbManagePlatform(
      PlatformRepository repository, PasswordHasher passwordHasher, Clock clock) {
    this.repository = repository;
    this.passwordHasher = passwordHasher;
    this.clock = clock;
  }

  @Override
  public User signIn(String email, String password) throws InvalidCredentialsError {
    String normalized = ContactRules.normalizeEmail(email);
    if (normalized == null || password == null) {
      throw new InvalidCredentialsError();
    }
    User admin = repository.loadAdminByEmail(normalized).orElseThrow(InvalidCredentialsError::new);
    String hash =
        repository.loadAdminPasswordHash(admin.id()).orElseThrow(InvalidCredentialsError::new);
    if (!passwordHasher.matches(password, hash)) {
      throw new InvalidCredentialsError();
    }
    return admin;
  }

  @Override
  public List<EstablishmentSummary> list() {
    return repository.listEstablishments();
  }

  @Override
  public EstablishmentSummary create(NewEstablishment params)
      throws EstablishmentSlugTakenError, EmailAlreadyRegisteredError {
    String name = EstablishmentRules.name(params.name());
    EstablishmentRules.hours(params.openingHour(), params.closingHour());
    EstablishmentRules.capacity(params.maxVehiclesPerHour());
    String slug = slug(params.slug() == null || params.slug().isBlank() ? name : params.slug());
    String managerName = ContactRules.limited(params.managerName(), 100, "Nome do gerente");
    if (managerName == null) {
      throw new IllegalArgumentException("Informe o nome do gerente");
    }
    String email = ContactRules.normalizeEmail(params.managerEmail());
    if (email == null) {
      throw new IllegalArgumentException("Informe o e-mail de acesso do gerente");
    }
    String phone = ContactRules.normalizePhone(params.managerPhone());
    if (params.managerPassword() == null || params.managerPassword().length() < 8) {
      throw new IllegalArgumentException("Senha do gerente deve ter no mínimo 8 caracteres");
    }
    if (repository.slugExists(slug)) {
      throw new EstablishmentSlugTakenError(slug);
    }
    if (repository.emailInUse(email)) {
      throw new EmailAlreadyRegisteredError();
    }
    Instant now = Instant.now(clock);
    return repository.createEstablishment(
        UUID.randomUUID(),
        slug,
        new Establishment(
            name,
            params.openingHour(),
            params.closingHour(),
            params.openOnSunday(),
            params.maxVehiclesPerHour(),
            now),
        new User(UUID.randomUUID(), managerName, phone, UserProfile.MANAGER, now, true, email),
        passwordHasher.hash(params.managerPassword()));
  }

  @Override
  public EstablishmentSummary setActive(UUID establishmentId, boolean active)
      throws EstablishmentNotFoundError {
    return repository
        .setEstablishmentActive(establishmentId, active)
        .orElseThrow(EstablishmentNotFoundError::new);
  }

  @Override
  public List<EstablishmentListing> search(String query) {
    String text = query == null ? "" : query.trim();
    return repository.searchActive(text.length() > 60 ? text.substring(0, 60) : text, SEARCH_LIMIT);
  }

  /** "Lava Rápido São José" -> "lava-rapido-sao-jose". */
  static String slug(String raw) {
    String plain =
        Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+|-+$)", "");
    if (plain.length() > SLUG_MAX) {
      plain = plain.substring(0, SLUG_MAX).replaceAll("-+$", "");
    }
    if (plain.length() < 2) {
      throw new IllegalArgumentException("Endereço do lava-jato inválido");
    }
    return plain;
  }
}
