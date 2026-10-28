package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.CountServiceOrdersScheduledInRangeRepository;
import com.washflow.data.protocols.db.CreateServiceOrderRepository;
import com.washflow.data.protocols.db.FinalizeServiceOrderRepository;
import com.washflow.data.protocols.db.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.LoadServiceOrdersByCustomerIdRepository;
import com.washflow.data.protocols.db.UpdateServiceOrderStatusRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.entities.ServicePayment;
import com.washflow.domain.entities.WasherCommission;
import com.washflow.infra.db.jdbi.OperationsReport.PauseBreakdown;
import com.washflow.infra.db.jdbi.OperationsReport.PaymentBreakdown;
import com.washflow.infra.db.jdbi.OperationsReport.ServiceBreakdown;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;

public class ServiceOrderJdbiRepository
    implements CreateServiceOrderRepository,
        LoadServiceOrderByIdRepository,
        LoadServiceOrdersByCustomerIdRepository,
        UpdateServiceOrderStatusRepository,
        CountServiceOrdersScheduledInRangeRepository,
        FinalizeServiceOrderRepository {

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
      SELECT a.id, a.veiculo_id, a.servico_preco_id, a.lavador_id, a.status,
             a.data_agendamento, a.criado_em, a.atualizado_em,
             (SELECT p.motivo FROM pausas_atendimento p
              WHERE p.atendimento_id = a.id AND p.retomada_em IS NULL) AS pause_reason
      FROM atendimentos a
      WHERE a.id = :id
      """;

  // lavador_id only changes when a washer is being assigned (DbStartServiceOrder) - every
  // other transition passes the vehicle's already-assigned washerId back in, so COALESCE
  // keeps the column as-is instead of overwriting it with null.
  private static final String UPDATE_STATUS_SQL =
      """
      UPDATE atendimentos
      SET status = :status,
          lavador_id = COALESCE(CAST(:washerId AS UUID), lavador_id),
          lavagem_iniciada_em = CASE
            WHEN :status = 'EM_LAVAGEM' AND lavagem_iniciada_em IS NULL THEN :updatedAt
            ELSE lavagem_iniciada_em
          END,
          lavagem_concluida_em = CASE
            WHEN :status = 'PRONTO' THEN :updatedAt
            ELSE lavagem_concluida_em
          END,
          atualizado_em = :updatedAt
      WHERE id = :id
      """;

  private static final String SELECT_BY_CUSTOMER_ID_SQL =
      """
      SELECT a.id, a.veiculo_id, a.servico_preco_id, a.lavador_id, a.status,
             a.data_agendamento, a.criado_em, a.atualizado_em,
             (SELECT p.motivo FROM pausas_atendimento p
              WHERE p.atendimento_id = a.id AND p.retomada_em IS NULL) AS pause_reason
      FROM atendimentos a
      JOIN veiculos v ON v.id = a.veiculo_id
      WHERE v.cliente_id = :customerId
      ORDER BY a.atualizado_em DESC
      """;

  private static final String COUNT_SCHEDULED_BETWEEN_SQL =
      "SELECT COUNT(*) FROM atendimentos WHERE data_agendamento >= :start AND data_agendamento < :end";

  private static final String WAITING_QUEUE_SQL =
      """
      SELECT a.id, a.veiculo_id, a.servico_preco_id, a.lavador_id, a.status,
             a.data_agendamento, a.criado_em, a.atualizado_em,
             (SELECT p.motivo FROM pausas_atendimento p
              WHERE p.atendimento_id = a.id AND p.retomada_em IS NULL) AS pause_reason
      FROM atendimentos a
      WHERE a.status = 'AGUARDANDO_PATIO'
      ORDER BY a.atualizado_em ASC
      """;

  private static final String YARD_OVERVIEW_SQL =
      """
      SELECT a.id, a.veiculo_id, a.servico_preco_id, a.lavador_id, a.status,
             a.data_agendamento, a.criado_em, a.atualizado_em,
             (SELECT p.motivo FROM pausas_atendimento p
              WHERE p.atendimento_id = a.id AND p.retomada_em IS NULL) AS pause_reason
      FROM atendimentos a
      WHERE a.status <> 'FINALIZADO'
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
      """
      SELECT a.id, a.veiculo_id, a.servico_preco_id, a.lavador_id, a.status,
             a.data_agendamento, a.criado_em, a.atualizado_em,
             (SELECT p.motivo FROM pausas_atendimento p
              WHERE p.atendimento_id = a.id AND p.retomada_em IS NULL) AS pause_reason
      FROM atendimentos a
      WHERE a.status IN ('EM_LAVAGEM', 'PAUSADA') AND a.lavador_id = :washerId
      ORDER BY a.atualizado_em ASC
      """;

  private static final ZoneId STORE_ZONE = ZoneId.of("America/Manaus");

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

  public List<ServiceOrder> loadWaitingQueue() {
    return jdbi.withHandle(
        handle -> handle.createQuery(WAITING_QUEUE_SQL).map(this::mapRow).list());
  }

  public List<ServiceOrder> loadYardOverview() {
    return jdbi.withHandle(
        handle -> handle.createQuery(YARD_OVERVIEW_SQL).map(this::mapRow).list());
  }

  public List<ServiceOrder> loadWashingByWasher(UUID washerId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(WASHER_ACTIVE_SQL)
                .bind("washerId", washerId)
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

  public boolean pauseWashing(UUID orderId, UUID washerId, String reason, String description) {
    return jdbi.inTransaction(
        handle -> {
          Instant now = Instant.now();
          int updated =
              handle
                  .createUpdate(
                      """
                      UPDATE atendimentos
                      SET status = 'PAUSADA', atualizado_em = :now
                      WHERE id = :id AND lavador_id = :washerId AND status = 'EM_LAVAGEM'
                      """)
                  .bind("now", now)
                  .bind("id", orderId)
                  .bind("washerId", washerId)
                  .execute();
          if (updated != 1) {
            return false;
          }
          handle
              .createUpdate(
                  """
                  INSERT INTO pausas_atendimento
                    (id, atendimento_id, lavador_id, motivo, descricao, iniciada_em)
                  VALUES (:id, :orderId, :washerId, :reason, :description, :startedAt)
                  """)
              .bind("id", UUID.randomUUID())
              .bind("orderId", orderId)
              .bind("washerId", washerId)
              .bind("reason", reason)
              .bind("description", description)
              .bind("startedAt", now)
              .execute();
          return true;
        });
  }

  public boolean resumeWashing(UUID orderId, UUID washerId) {
    return jdbi.inTransaction(
        handle -> {
          Instant now = Instant.now();
          int updated =
              handle
                  .createUpdate(
                      """
                      UPDATE atendimentos
                      SET status = 'EM_LAVAGEM', atualizado_em = :now
                      WHERE id = :id AND lavador_id = :washerId AND status = 'PAUSADA'
                      """)
                  .bind("now", now)
                  .bind("id", orderId)
                  .bind("washerId", washerId)
                  .execute();
          if (updated != 1) {
            return false;
          }
          int resumed =
              handle
                  .createUpdate(
                      """
                      UPDATE pausas_atendimento
                      SET retomada_em = :now
                      WHERE atendimento_id = :id AND lavador_id = :washerId
                        AND retomada_em IS NULL
                      """)
                  .bind("now", now)
                  .bind("id", orderId)
                  .bind("washerId", washerId)
                  .execute();
          if (resumed != 1) {
            throw new IllegalStateException("Active wash pause was not found");
          }
          return true;
        });
  }

  public OperationsReport loadOperationsReport(LocalDate from, LocalDate to) {
    Instant start = from.atStartOfDay(STORE_ZONE).toInstant();
    Instant end = to.plusDays(1).atStartOfDay(STORE_ZONE).toInstant();
    return jdbi.withHandle(
        handle -> {
          List<ReportOrder> orders =
              handle
                  .createQuery(
                      """
                      SELECT a.id, s.nome AS service_name, p.forma_pagamento, p.valor,
                             c.valor AS commission, a.lavagem_iniciada_em,
                             a.lavagem_concluida_em
                      FROM pagamentos p
                      JOIN atendimentos a ON a.id = p.atendimento_id
                      JOIN servicos_preco s ON s.id = a.servico_preco_id
                      LEFT JOIN comissoes_lavador c ON c.atendimento_id = a.id
                      WHERE p.recebido_em >= :start AND p.recebido_em < :end
                      ORDER BY p.recebido_em ASC
                      """)
                  .bind("start", start)
                  .bind("end", end)
                  .map(
                      (rs, ctx) ->
                          new ReportOrder(
                              rs.getObject("id", UUID.class),
                              rs.getString("service_name"),
                              rs.getString("forma_pagamento"),
                              rs.getBigDecimal("valor"),
                              rs.getBigDecimal("commission"),
                              timestamp(rs, "lavagem_iniciada_em"),
                              timestamp(rs, "lavagem_concluida_em")))
                  .list();

          Map<UUID, Duration> pauseDurations = new LinkedHashMap<>();
          Map<String, Integer> pauseCounts = new LinkedHashMap<>();
          if (!orders.isEmpty()) {
            List<UUID> orderIds = orders.stream().map(ReportOrder::id).toList();
            handle
                .createQuery(
                    """
                    SELECT atendimento_id, motivo, iniciada_em, retomada_em
                    FROM pausas_atendimento
                    WHERE atendimento_id IN (<orderIds>)
                    """)
                .bindList("orderIds", orderIds)
                .map(
                    (rs, ctx) -> {
                      UUID orderId = rs.getObject("atendimento_id", UUID.class);
                      Instant startedAt = rs.getTimestamp("iniciada_em").toInstant();
                      java.sql.Timestamp resumed = rs.getTimestamp("retomada_em");
                      Instant endedAt = resumed == null ? Instant.now() : resumed.toInstant();
                      pauseDurations.merge(
                          orderId, Duration.between(startedAt, endedAt), Duration::plus);
                      pauseCounts.merge(rs.getString("motivo"), 1, Integer::sum);
                      return orderId;
                    })
                .list();
          }

          BigDecimal revenue = BigDecimal.ZERO;
          BigDecimal commissions = BigDecimal.ZERO;
          long totalWashingMinutes = 0;
          int timedOrders = 0;
          Map<String, ServiceSummary> services = new LinkedHashMap<>();
          Map<String, PaymentSummary> payments = new LinkedHashMap<>();
          for (ReportOrder order : orders) {
            revenue = revenue.add(order.amount());
            if (order.commission() != null) {
              commissions = commissions.add(order.commission());
            }
            services
                .computeIfAbsent(order.serviceName(), ignored -> new ServiceSummary())
                .add(order.amount(), order.commission());
            payments
                .computeIfAbsent(order.paymentMethod(), ignored -> new PaymentSummary())
                .add(order.amount());
            if (order.washStartedAt() != null && order.washCompletedAt() != null) {
              Duration elapsed = Duration.between(order.washStartedAt(), order.washCompletedAt());
              Duration paused = pauseDurations.getOrDefault(order.id(), Duration.ZERO);
              totalWashingMinutes += Math.max(0, elapsed.minus(paused).toMinutes());
              timedOrders++;
            }
          }

          List<ServiceBreakdown> serviceBreakdowns = new ArrayList<>();
          services.forEach(
              (name, summary) ->
                  serviceBreakdowns.add(
                      new ServiceBreakdown(
                          name, summary.orders, summary.revenue, summary.commission)));
          List<PaymentBreakdown> paymentBreakdowns = new ArrayList<>();
          payments.forEach(
              (method, summary) ->
                  paymentBreakdowns.add(
                      new PaymentBreakdown(method, summary.orders, summary.amount)));
          List<PauseBreakdown> pauseBreakdowns = new ArrayList<>();
          pauseCounts.forEach(
              (reason, count) -> pauseBreakdowns.add(new PauseBreakdown(reason, count)));
          return new OperationsReport(
              from,
              to,
              orders.size(),
              revenue,
              commissions,
              timedOrders == 0 ? null : totalWashingMinutes / timedOrders,
              serviceBreakdowns,
              paymentBreakdowns,
              pauseBreakdowns);
        });
  }

  private static Instant timestamp(ResultSet rs, String column) throws SQLException {
    java.sql.Timestamp value = rs.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }

  private record ReportOrder(
      UUID id,
      String serviceName,
      String paymentMethod,
      BigDecimal amount,
      BigDecimal commission,
      Instant washStartedAt,
      Instant washCompletedAt) {}

  private static final class ServiceSummary {
    private int orders;
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal commission = BigDecimal.ZERO;

    private void add(BigDecimal amount, BigDecimal commissionAmount) {
      orders++;
      revenue = revenue.add(amount);
      if (commissionAmount != null) {
        commission = commission.add(commissionAmount);
      }
    }
  }

  private static final class PaymentSummary {
    private int orders;
    private BigDecimal amount = BigDecimal.ZERO;

    private void add(BigDecimal value) {
      orders++;
      amount = amount.add(value);
    }
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

  @Override
  public ServiceOrder finalizeSale(
      ServiceOrder serviceOrder, ServicePayment payment, WasherCommission commission) {
    return jdbi.inTransaction(
        handle -> {
          int updated =
              handle
                  .createUpdate(
                      """
                      UPDATE atendimentos
                      SET status = 'FINALIZADO', atualizado_em = :updatedAt
                      WHERE id = :id AND status = 'PRONTO'
                      """)
                  .bind("updatedAt", payment.receivedAt())
                  .bind("id", serviceOrder.id())
                  .execute();
          if (updated != 1) {
            throw new IllegalStateException("Service order was finalized concurrently");
          }
          handle
              .createUpdate(
                  """
                  INSERT INTO pagamentos (id, atendimento_id, forma_pagamento, valor, recebido_em)
                  VALUES (:id, :serviceOrderId, :method, :amount, :receivedAt)
                  """)
              .bind("id", payment.id())
              .bind("serviceOrderId", payment.serviceOrderId())
              .bind("method", payment.method().name())
              .bind("amount", payment.amount())
              .bind("receivedAt", payment.receivedAt())
              .execute();
          handle
              .createUpdate(
                  """
                  INSERT INTO comissoes_lavador (id, atendimento_id, lavador_id, valor, criado_em)
                  VALUES (:id, :serviceOrderId, :washerId, :amount, :creditedAt)
                  """)
              .bind("id", commission.id())
              .bind("serviceOrderId", commission.serviceOrderId())
              .bind("washerId", commission.washerId())
              .bind("amount", commission.amount())
              .bind("creditedAt", commission.creditedAt())
              .execute();
          return handle
              .createQuery(SELECT_SQL)
              .bind("id", serviceOrder.id())
              .map(this::mapRow)
              .one();
        });
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
        rs.getTimestamp("atualizado_em").toInstant(),
        rs.getString("pause_reason"));
  }
}
