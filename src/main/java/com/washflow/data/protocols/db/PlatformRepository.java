package com.washflow.data.protocols.db;

import com.washflow.domain.entities.Establishment;
import com.washflow.domain.entities.EstablishmentListing;
import com.washflow.domain.entities.EstablishmentSummary;
import com.washflow.domain.entities.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Platform-level data: administrators and the establishments themselves. */
public interface PlatformRepository {

  boolean adminExists();

  /** Creates the first administrator; false when one already exists (setup ran twice). */
  boolean createFirstAdmin(User admin, String passwordHash);

  Optional<User> loadAdminByEmail(String email);

  Optional<String> loadAdminPasswordHash(UUID adminId);

  boolean slugExists(String slug);

  boolean emailInUse(String email);

  /**
   * Creates the establishment, its first manager and a starter catalog (services and expense
   * categories) in one transaction.
   */
  EstablishmentSummary createEstablishment(
      UUID id, String slug, Establishment settings, User manager, String managerPasswordHash);

  List<EstablishmentSummary> listEstablishments();

  Optional<EstablishmentSummary> setEstablishmentActive(UUID id, boolean active);

  List<EstablishmentListing> searchActive(String query, int limit);
}
