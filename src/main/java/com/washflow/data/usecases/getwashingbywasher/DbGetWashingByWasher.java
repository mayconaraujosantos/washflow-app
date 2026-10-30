package com.washflow.data.usecases.getwashingbywasher;

import com.washflow.data.protocols.db.LoadWashingByWasherRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.usecases.GetWashingByWasher;
import java.util.List;
import java.util.UUID;

/**
 * Delegates to {@code LoadWashingByWasherRepository}. Depends only on the port interface, never on
 * JDBI or Postgres.
 */
public class DbGetWashingByWasher implements GetWashingByWasher {

  private final LoadWashingByWasherRepository loadRepository;

  public DbGetWashingByWasher(LoadWashingByWasherRepository loadRepository) {
    this.loadRepository = loadRepository;
  }

  @Override
  public List<ServiceOrder> get(UUID washerId) {
    return loadRepository.loadByWasherId(washerId);
  }
}
