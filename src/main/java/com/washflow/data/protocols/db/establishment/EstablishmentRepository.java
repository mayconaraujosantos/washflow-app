package com.washflow.data.protocols.db.establishment;

import com.washflow.domain.entities.Establishment;

public interface EstablishmentRepository {

  Establishment load();

  Establishment update(Establishment establishment);

  /**
   * Atomically stores the configured establishment and creates its owner; fails when another
   * request configured it first.
   */
}
