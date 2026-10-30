package com.washflow.data.usecases.getwaitingqueue;

import com.washflow.data.protocols.db.LoadWaitingQueueRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.usecases.GetWaitingQueue;
import java.util.List;

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
  public List<ServiceOrder> get() {
    return loadRepository.loadWaitingQueue();
  }
}
