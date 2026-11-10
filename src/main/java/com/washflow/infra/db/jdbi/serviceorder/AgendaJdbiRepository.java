package com.washflow.infra.db.jdbi.serviceorder;

import com.washflow.data.protocols.db.schedule.CountServiceOrdersScheduledInRangeRepository;
import com.washflow.data.protocols.db.schedule.LoadServiceOrdersScheduledBetweenRepository;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import java.time.Instant;
import java.util.List;
import org.jdbi.v3.core.Jdbi;

/** The agenda's reads: orders scheduled in a range and how many take an hourly slot. */
public class AgendaJdbiRepository
    implements LoadServiceOrdersScheduledBetweenRepository,
        CountServiceOrdersScheduledInRangeRepository {

  private static final String SCHEDULED_BETWEEN_SQL =
      ServiceOrderRows.SELECT_WITH_DETAILS
          + """
      WHERE a.data_agendamento >= :start AND a.data_agendamento < :end AND a.estabelecimento_id = :tenant
      ORDER BY a.data_agendamento ASC
      """;

  private final Jdbi jdbi;

  public AgendaJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public List<ServiceOrder> loadScheduledBetween(Instant start, Instant end) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SCHEDULED_BETWEEN_SQL)
                .bind("start", start)
                .bind("end", end)
                .map(ServiceOrderRows::map)
                .list());
  }

  @Override
  public int countScheduledBetween(Instant start, Instant end) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(ServiceOrderRows.COUNT_SCHEDULED_BETWEEN_SQL)
                .bind("start", start)
                .bind("end", end)
                .mapTo(Integer.class)
                .one());
  }
}
