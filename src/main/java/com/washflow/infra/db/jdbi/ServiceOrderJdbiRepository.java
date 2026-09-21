package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.CountServiceOrdersScheduledInRangeRepository;
import com.washflow.data.protocols.db.CreateServiceOrderRepository;
import com.washflow.data.protocols.db.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.LoadServiceOrdersByCustomerIdRepository;
import com.washflow.data.protocols.db.UpdateServiceOrderStatusRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;

public class ServiceOrderJdbiRepository
    implements CreateServiceOrderRepository,
        LoadServiceOrderByIdRepository,
        LoadServiceOrdersByCustomerIdRepository,
        UpdateServiceOrderStatusRepository,
        CountServiceOrdersScheduledInRangeRepository {

  private static final String STATUS = "status";

  private static final String INSERT_SQL =
      """
      INSERT INTO atendimentos
        (id, veiculo_id, servico_preco_id, status, data_agendamento, criado_em, atualizado_em)
      VALUES
        (:id, :vehicleId, :servicePriceId, :status, :scheduledAt, :createdAt, :updatedAt)
      """;

  private static final String SELECT_SQL =
      """
      SELECT id, veiculo_id, servico_preco_id, lavador_id, status, data_agendamento, criado_em, atualizado_em
      FROM atendimentos
      WHERE id = :id
      """;

  // lavador_id only changes when a washer is being assigned (DbStartServiceOrder) - every
  // other transition passes the vehicle's already-assigned washerId back in, so COALESCE
  // keeps the column as-is instead of overwriting it with null.
  private static final String UPDATE_STATUS_SQL =
      """
      UPDATE atendimentos
      SET status = :status,
          lavador_id = COALESCE(:washerId, lavador_id),
          atualizado_em = :updatedAt
      WHERE id = :id
      """;

  private static final String SELECT_BY_CUSTOMER_ID_SQL =
      """
      SELECT a.id, a.veiculo_id, a.servico_preco_id, a.lavador_id, a.status, a.data_agendamento, a.criado_em, a.atualizado_em
      FROM atendimentos a
      JOIN veiculos v ON v.id = a.veiculo_id
      WHERE v.cliente_id = :customerId
      ORDER BY a.atualizado_em DESC
      """;

  private static final String COUNT_SCHEDULED_BETWEEN_SQL =
      "SELECT COUNT(*) FROM atendimentos WHERE data_agendamento >= :start AND data_agendamento < :end";

  private final Jdbi jdbi;

  public ServiceOrderJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public ServiceOrder create(ServiceOrder serviceOrder) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(INSERT_SQL)
                .bind("id", serviceOrder.id())
                .bind("vehicleId", serviceOrder.vehicleId())
                .bind("servicePriceId", serviceOrder.servicePriceId())
                .bind(STATUS, serviceOrder.status().dbValue())
                .bind("scheduledAt", serviceOrder.scheduledAt())
                .bind("createdAt", serviceOrder.createdAt())
                .bind("updatedAt", serviceOrder.updatedAt())
                .execute());

    return serviceOrder;
  }

  @Override
  public Optional<ServiceOrder> loadById(UUID id) {
    return jdbi.withHandle(
        handle -> handle.createQuery(SELECT_SQL).bind("id", id).map(this::mapRow).findOne());
  }

  @Override
  public List<ServiceOrder> loadByCustomerId(UUID customerId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_BY_CUSTOMER_ID_SQL)
                .bind("customerId", customerId)
                .map(this::mapRow)
                .list());
  }

  @Override
  public ServiceOrder updateStatus(UUID id, ServiceOrderStatus status, UUID washerId) {
    return jdbi.inTransaction(
        handle -> {
          handle
              .createUpdate(UPDATE_STATUS_SQL)
              .bind("id", id)
              .bind(STATUS, status.dbValue())
              .bind("washerId", washerId)
              .bind("updatedAt", Instant.now())
              .execute();

          return handle.createQuery(SELECT_SQL).bind("id", id).map(this::mapRow).one();
        });
  }

  @Override
  public int countScheduledBetween(Instant start, Instant end) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(COUNT_SCHEDULED_BETWEEN_SQL)
                .bind("start", start)
                .bind("end", end)
                .mapTo(Integer.class)
                .one());
  }

  private ServiceOrder mapRow(ResultSet rs, StatementContext ctx) throws SQLException {
    return new ServiceOrder(
        (UUID) rs.getObject("id"),
        (UUID) rs.getObject("veiculo_id"),
        rs.getInt("servico_preco_id"),
        (UUID) rs.getObject("lavador_id"),
        ServiceOrderStatus.fromDbValue(rs.getString(STATUS)),
        rs.getTimestamp("data_agendamento").toInstant(),
        rs.getTimestamp("criado_em").toInstant(),
        rs.getTimestamp("atualizado_em").toInstant());
  }
}
