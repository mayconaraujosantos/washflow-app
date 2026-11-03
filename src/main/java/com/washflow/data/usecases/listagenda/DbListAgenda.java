package com.washflow.data.usecases.listagenda;

import com.washflow.data.protocols.db.LoadServiceOrdersScheduledBetweenRepository;
import com.washflow.data.usecases.scheduleserviceorder.SchedulingRules;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.usecases.ListAgenda;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class DbListAgenda implements ListAgenda {

  private static final int MAX_DAYS = 62;

  private final LoadServiceOrdersScheduledBetweenRepository repository;

  public DbListAgenda(LoadServiceOrdersScheduledBetweenRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<ServiceOrder> list(Params params) {
    if (params.to().isBefore(params.from())
        || ChronoUnit.DAYS.between(params.from(), params.to()) >= MAX_DAYS) {
      throw new IllegalArgumentException(
          "Agenda range must be ordered and no longer than " + MAX_DAYS + " days");
    }
    Instant start = params.from().atStartOfDay(SchedulingRules.STORE_ZONE).toInstant();
    Instant end = params.to().plusDays(1).atStartOfDay(SchedulingRules.STORE_ZONE).toInstant();
    return repository.loadScheduledBetween(start, end);
  }
}
