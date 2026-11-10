package com.washflow.infra.db.jdbi.serviceorder;

import com.washflow.data.protocols.db.yard.LoadWaitingQueueRepository;
import com.washflow.data.protocols.db.yard.LoadWashingByWasherRepository;
import com.washflow.data.protocols.db.yard.LoadYardOverviewRepository;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import java.util.List;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

/** The yard screens' reads: waiting queue, yard overview and a washer's active washes. */
public class YardJdbiRepository
    implements LoadWaitingQueueRepository,
        LoadYardOverviewRepository,
        LoadWashingByWasherRepository {

  private static final String WAITING_QUEUE_SQL =
      ServiceOrderRows.SELECT_WITH_DETAILS
          + """
      WHERE a.status = 'AGUARDANDO_PATIO' AND a.estabelecimento_id = :tenant
      ORDER BY a.atualizado_em ASC
      """;

  private static final String YARD_OVERVIEW_SQL =
      ServiceOrderRows.SELECT_WITH_DETAILS
          + """
      WHERE a.status NOT IN ('FINALIZADO', 'CANCELADO') AND a.estabelecimento_id = :tenant
      ORDER BY
        CASE a.status
          WHEN 'EM_LAVAGEM' THEN 0
          WHEN 'PAUSADA' THEN 0
          WHEN 'AGUARDANDO_PATIO' THEN 1
          WHEN 'PRONTO' THEN 2
          WHEN 'AGENDADO' THEN 3
          ELSE 4
        END,
        a.atualizado_em ASC
      """;

  private static final String WASHER_ACTIVE_SQL =
      ServiceOrderRows.SELECT_WITH_DETAILS
          + """
      WHERE a.status IN ('EM_LAVAGEM', 'PAUSADA') AND a.lavador_id = :washerId
        AND a.estabelecimento_id = :tenant
      ORDER BY a.atualizado_em ASC
      """;

  private final Jdbi jdbi;

  public YardJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public List<ServiceOrder> loadWaitingQueue() {
    return jdbi.withHandle(
        handle -> handle.createQuery(WAITING_QUEUE_SQL).map(ServiceOrderRows::map).list());
  }

  @Override
  public List<ServiceOrder> loadYardOverview() {
    return jdbi.withHandle(
        handle -> handle.createQuery(YARD_OVERVIEW_SQL).map(ServiceOrderRows::map).list());
  }

  @Override
  public List<ServiceOrder> loadByWasherId(UUID washerId) {
    return loadWashingByWasher(washerId);
  }

  public List<ServiceOrder> loadWashingByWasher(UUID washerId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(WASHER_ACTIVE_SQL)
                .bind("washerId", washerId)
                .map(ServiceOrderRows::map)
                .list());
  }
}
