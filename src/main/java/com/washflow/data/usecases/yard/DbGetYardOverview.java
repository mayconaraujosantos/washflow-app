package com.washflow.data.usecases.yard;

import com.washflow.data.protocols.db.LoadYardOverviewRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.usecases.yard.GetYardOverview;
import java.util.List;

/**
 * Delegates to {@code LoadYardOverviewRepository}. Depends only on the port interface, never on
 * JDBI or Postgres.
 */
public class DbGetYardOverview implements GetYardOverview {

  private final LoadYardOverviewRepository loadRepository;

  public DbGetYardOverview(LoadYardOverviewRepository loadRepository) {
    this.loadRepository = loadRepository;
  }

  @Override
  public List<ServiceOrder> get() {
    return loadRepository.loadYardOverview();
  }
}
