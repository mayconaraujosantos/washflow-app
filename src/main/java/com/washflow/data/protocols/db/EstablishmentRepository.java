package com.washflow.data.protocols.db;

import com.washflow.domain.entities.Establishment;
import com.washflow.domain.entities.User;
import com.washflow.domain.errors.EstablishmentAlreadyConfiguredError;

public interface EstablishmentRepository {

  Establishment load();

  Establishment update(Establishment establishment);

  /**
   * Atomically stores the configured establishment and creates its owner; fails when another
   * request configured it first.
   */
  User completeSetup(Establishment establishment, User owner, String passwordHash)
      throws EstablishmentAlreadyConfiguredError;
}
