package com.washflow.data.usecases.yard;

import com.washflow.data.protocols.db.yard.LoadWaitingQueueRepository;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.usecases.yard.GetWaitingQueue;
import java.util.List;
import java.util.UUID;

/**
 * Delegates to {@code LoadWaitingQueueRepository}. Depends only on the port interface, never on
 * JDBI or Postgres.
 */
public class DbGetWaitingQueue implements GetWaitingQueue {

  private final LoadWaitingQueueRepository loadRepository;

  public DbGetWaitingQueue(LoadWaitingQueueRepository loadRepository) {
    this.loadRepository = loadRepository;
  }

  @Override
  public List<ServiceOrder> get(UUID washerId) {
    return loadRepository.loadWaitingQueue(washerId);
  }
}
