package com.washflow.data.usecases.listchecklists;

import com.washflow.data.protocols.db.LoadServiceOrderChecklistRepository;
import com.washflow.domain.entities.ServiceOrderChecklist;
import com.washflow.domain.usecases.ListServiceOrderChecklists;
import java.util.List;
import java.util.UUID;

/**
 * Delegates to {@code LoadServiceOrderChecklistRepository}. Depends only on the port interface,
 * never on JDBI or Postgres.
 */
public class DbListServiceOrderChecklists implements ListServiceOrderChecklists {

  private final LoadServiceOrderChecklistRepository loadRepository;

  public DbListServiceOrderChecklists(LoadServiceOrderChecklistRepository loadRepository) {
    this.loadRepository = loadRepository;
  }

  @Override
  public List<ServiceOrderChecklist> list(UUID serviceOrderId) {
    return loadRepository.loadByServiceOrderId(serviceOrderId);
  }
}
