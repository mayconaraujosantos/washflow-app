package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.serviceorder.CancelServiceOrderRepository;
import com.washflow.data.protocols.db.serviceorder.CreateServiceOrderRepository;
import com.washflow.data.protocols.db.serviceorder.FinalizeServiceOrderRepository;
import com.washflow.data.protocols.db.serviceorder.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.serviceorder.LoadServiceOrdersByCustomerIdRepository;
import com.washflow.data.protocols.db.serviceorder.PauseServiceOrderRepository;
import com.washflow.data.protocols.db.serviceorder.RefundServiceOrderRepository;
import com.washflow.data.protocols.db.serviceorder.ResumeServiceOrderRepository;
import com.washflow.data.protocols.db.serviceorder.UpdateServiceOrderStatusRepository;
import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderRefund;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.entities.ServicePayment;
import com.washflow.domain.entities.WasherCommission;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

/**
 * The service order lifecycle: creation (with the hourly capacity lock), lookups and the
 * compare-and-set status transitions - start, complete, pause, resume, cancel, refund and finalize.
 */
public class ServiceOrderJdbiRepository
    implements CreateServiceOrderRepository,
        LoadServiceOrderByIdRepository,
        LoadServiceOrdersByCustomerIdRepository,
        UpdateServiceOrderStatusRepository,
        FinalizeServiceOrderRepository,
        PauseServiceOrderRepository,
        ResumeServiceOrderRepository,
        CancelServiceOrderRepository,
        RefundServiceOrderRepository {

  private static final String STATUS = "status";

  private static final String INSERT_SQL =
      """
      INSERT INTO atendimentos
        (id, estabelecimento_id, veiculo_id, servico_preco_id, status, data_agendamento,
         criado_em, atualizado_em, previsao_entrega, observacoes)
      VALUES
        (:id, :tenant, :vehicleId, :servicePriceId, :status, :scheduledAt, :createdAt,
         :updatedAt, :expectedDeliveryAt, :notes)
      """;

  private static final String SELECT_SQL =
      ServiceOrderRows.SELECT_WITH_DETAILS
          + """
      WHERE a.id = :id AND a.estabelecimento_id = :tenant
      """;

  // lavador_id only changes when a washer is being assigned (DbStartServiceOrder) - every
  // other transition passes the vehicle's already-assigned washerId back in, so COALESCE
  // keeps the column as-is instead of overwriting it with null. "status = :expected" makes the
  // transition a compare-and-set: of two requests racing for the same order, only one updates.
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
      WHERE id = :id AND status = :expected AND estabelecimento_id = :tenant
      """;

  private static final String SELECT_BY_CUSTOMER_ID_SQL =
      ServiceOrderRows.SELECT_WITH_DETAILS
          + """
      WHERE v.cliente_id = :customerId AND a.estabelecimento_id = :tenant
      ORDER BY a.atualizado_em DESC
      """;

  // Transaction-scoped advisory lock keyed by lava-jato + slot start (see createWithinCapacity).
  private static final String SLOT_LOCK_SQL =
      "SELECT 1 FROM (SELECT pg_advisory_xact_lock(hashtextextended("
          + "CAST(:tenant AS TEXT) || '|' || :slotKey, 0))) AS slot_lock";

  private final Jdbi jdbi;

  public ServiceOrderJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public ServiceOrder create(ServiceOrder serviceOrder) {
    UniqueViolations.translate(() -> jdbi.useHandle(handle -> insert(handle, serviceOrder)));
    return serviceOrder;
  }

  @Override
  public Optional<ServiceOrder> createWithinCapacity(
      ServiceOrder serviceOrder, Instant slotStart, Instant slotEnd, int capacity) {
    return UniqueViolations.translate(
        () ->
            jdbi.inTransaction(
                handle -> {
                  // One lock per lava-jato and hour, held until commit: concurrent bookings of
                  // the same slot count and insert one at a time; other slots aren't blocked.
                  handle
                      .createQuery(SLOT_LOCK_SQL)
                      .bind("slotKey", slotStart.toString())
                      .mapTo(Integer.class)
                      .one();
                  int booked =
                      handle
                          .createQuery(ServiceOrderRows.COUNT_SCHEDULED_BETWEEN_SQL)
                          .bind("start", slotStart)
                          .bind("end", slotEnd)
                          .mapTo(Integer.class)
                          .one();
                  if (booked >= capacity) {
                    return Optional.<ServiceOrder>empty();
                  }
                  insert(handle, serviceOrder);
                  return Optional.of(serviceOrder);
                }));
  }

  private static void insert(Handle handle, ServiceOrder serviceOrder) {
    handle
        .createUpdate(INSERT_SQL)
        .bind("id", serviceOrder.id())
        .bind("vehicleId", serviceOrder.vehicleId())
        .bind("servicePriceId", serviceOrder.servicePriceId())
        .bind(STATUS, serviceOrder.status().dbValue())
        .bind("scheduledAt", serviceOrder.scheduledAt())
        .bind("createdAt", serviceOrder.createdAt())
        .bind("updatedAt", serviceOrder.updatedAt())
        .bind("expectedDeliveryAt", serviceOrder.expectedDeliveryAt())
        .bind("notes", serviceOrder.notes())
        .execute();
  }

  @Override
  public Optional<ServiceOrder> loadById(UUID id) {
    return jdbi.withHandle(
        handle ->
            handle.createQuery(SELECT_SQL).bind("id", id).map(ServiceOrderRows::map).findOne());
  }

  @Override
  public List<ServiceOrder> loadByCustomerId(UUID customerId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_BY_CUSTOMER_ID_SQL)
                .bind("customerId", customerId)
                .map(ServiceOrderRows::map)
                .list());
  }

  @Override
  public Optional<ServiceOrder> updateStatus(
      UUID id, ServiceOrderStatus expected, ServiceOrderStatus status, UUID washerId) {
    return UniqueViolations.translate(
        () ->
            jdbi.inTransaction(
                handle -> {
                  int updated =
                      handle
                          .createUpdate(UPDATE_STATUS_SQL)
                          .bind("id", id)
                          .bind("expected", expected.dbValue())
                          .bind(STATUS, status.dbValue())
                          .bind("washerId", washerId)
                          .bind("updatedAt", Instant.now())
                          .execute();
                  if (updated != 1) {
                    return Optional.<ServiceOrder>empty();
                  }
                  return Optional.of(
                      handle
                          .createQuery(SELECT_SQL)
                          .bind("id", id)
                          .map(ServiceOrderRows::map)
                          .one());
                }));
  }

  @Override
  public boolean pause(UUID orderId, UUID washerId, String reason, String description) {
    return pauseWashing(orderId, washerId, reason, description);
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
                        AND estabelecimento_id = :tenant
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

  @Override
  public boolean resume(UUID orderId, UUID washerId) {
    return resumeWashing(orderId, washerId);
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
                        AND estabelecimento_id = :tenant
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

  // Compare-and-set like every transition: only orders that never reached washing are cancelled,
  // so a manager racing a washer who just started the car gets a 409 instead of a lost wash.
  @Override
  public boolean cancel(UUID orderId, UUID managerId, String reason, String description) {
    return jdbi.withHandle(
            handle ->
                handle
                    .createUpdate(
                        """
                        UPDATE atendimentos
                        SET status = 'CANCELADO', atualizado_em = :now, cancelado_em = :now,
                            cancelado_por = :managerId, motivo_cancelamento = :reason,
                            descricao_cancelamento = :description
                        WHERE id = :id AND status IN ('AGENDADO', 'AGUARDANDO_PATIO')
                          AND estabelecimento_id = :tenant
                        """)
                    .bind("now", Instant.now())
                    .bind("managerId", managerId)
                    .bind("reason", reason)
                    .bind("description", description)
                    .bind("id", orderId)
                    .execute())
        == 1;
  }

  // The status compare-and-set (FINALIZADO -> ESTORNADO) makes a double click refund only once;
  // the amount always comes from the recorded payment, never from the request.
  @Override
  public Optional<ServiceOrderRefund> refund(
      UUID orderId,
      UUID managerId,
      String reason,
      String description,
      PaymentMethod refundMethod,
      boolean reverseCommission) {
    return jdbi.inTransaction(
        handle -> {
          Instant now = Instant.now();
          int updated =
              handle
                  .createUpdate(
                      """
                      UPDATE atendimentos SET status = 'ESTORNADO', atualizado_em = :now
                      WHERE id = :id AND status = 'FINALIZADO' AND estabelecimento_id = :tenant
                      """)
                  .bind("now", now)
                  .bind("id", orderId)
                  .execute();
          if (updated != 1) {
            return Optional.<ServiceOrderRefund>empty();
          }
          Map<String, Object> paid =
              handle
                  .createQuery(
                      """
                      SELECT p.valor, p.forma_pagamento,
                             (SELECT COALESCE(SUM(c.valor), 0) FROM comissoes_lavador c
                              WHERE c.atendimento_id = p.atendimento_id) AS commission
                      FROM pagamentos p WHERE p.atendimento_id = :id
                      """)
                  .bind("id", orderId)
                  .mapToMap()
                  .one();
          ServiceOrderRefund refund =
              new ServiceOrderRefund(
                  UUID.randomUUID(),
                  orderId,
                  (BigDecimal) paid.get("valor"),
                  refundMethod != null
                      ? refundMethod
                      : PaymentMethod.valueOf((String) paid.get("forma_pagamento")),
                  reason,
                  description,
                  reverseCommission
                      ? ((BigDecimal) paid.get("commission")).setScale(2)
                      : BigDecimal.ZERO.setScale(2),
                  managerId,
                  now);
          handle
              .createUpdate(
                  """
                  INSERT INTO estornos_atendimento
                    (id, estabelecimento_id, atendimento_id, valor, forma_devolucao, motivo,
                     descricao, comissao_estornada, criado_por, criado_em)
                  VALUES (:id, :tenant, :orderId, :amount, :method, :reason, :description,
                          :reversedCommission, :createdBy, :createdAt)
                  """)
              .bind("id", refund.id())
              .bind("orderId", orderId)
              .bind("amount", refund.amount())
              .bind("method", refund.method().name())
              .bind("reason", reason)
              .bind("description", description)
              .bind("reversedCommission", refund.reversedCommission())
              .bind("createdBy", managerId)
              .bind("createdAt", now)
              .execute();
          return Optional.of(refund);
        });
  }

  @Override
  public Optional<ServiceOrder> finalizeSale(
      ServiceOrder serviceOrder, ServicePayment payment, WasherCommission commission) {
    return jdbi.inTransaction(
        handle -> {
          int updated =
              handle
                  .createUpdate(
                      """
                      UPDATE atendimentos
                      SET status = 'FINALIZADO', atualizado_em = :updatedAt
                      WHERE id = :id AND status = 'PRONTO' AND estabelecimento_id = :tenant
                      """)
                  .bind("updatedAt", payment.receivedAt())
                  .bind("id", serviceOrder.id())
                  .execute();
          if (updated != 1) {
            return Optional.<ServiceOrder>empty(); // finalized by a concurrent request
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
          return Optional.of(
              handle
                  .createQuery(SELECT_SQL)
                  .bind("id", serviceOrder.id())
                  .map(ServiceOrderRows::map)
                  .one());
        });
  }
}
