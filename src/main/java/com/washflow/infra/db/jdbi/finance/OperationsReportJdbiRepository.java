package com.washflow.infra.db.jdbi.finance;

import com.washflow.data.protocols.db.finance.LoadOperationsReportRepository;
import com.washflow.domain.entities.finance.OperationsReport;
import com.washflow.domain.entities.finance.OperationsReport.PauseBreakdown;
import com.washflow.domain.entities.finance.OperationsReport.PaymentBreakdown;
import com.washflow.domain.entities.finance.OperationsReport.ReasonCount;
import com.washflow.domain.entities.finance.OperationsReport.RefundBreakdown;
import com.washflow.domain.entities.finance.OperationsReport.ServiceBreakdown;
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
import java.util.UUID;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

/**
 * The manager's operations report: payments, commissions, washing time and pauses of the period,
 * plus refunds, cancellations and quick sales counted on the day they happened.
 */
public class OperationsReportJdbiRepository implements LoadOperationsReportRepository {

  private static final ZoneId STORE_ZONE = ZoneId.of("America/Manaus");

  private static final String START = "start";
  private static final String END = "end";

  private static final String PAID_ORDERS_SQL =
      """
      SELECT a.id, s.nome AS service_name, p.forma_pagamento, p.valor,
             c.valor AS commission, a.lavagem_iniciada_em,
             a.lavagem_concluida_em
      FROM pagamentos p
      JOIN atendimentos a ON a.id = p.atendimento_id
      JOIN servicos_preco s ON s.id = a.servico_preco_id
      LEFT JOIN comissoes_lavador c ON c.atendimento_id = a.id
      WHERE p.recebido_em >= :start AND p.recebido_em < :end
        AND a.estabelecimento_id = :tenant
      ORDER BY p.recebido_em ASC
      """;

  private static final String PAUSES_SQL =
      """
      SELECT atendimento_id, motivo, iniciada_em, retomada_em
      FROM pausas_atendimento
      WHERE atendimento_id IN (<orderIds>)
      """;

  private static final String CANCELLATIONS_SQL =
      """
      SELECT motivo_cancelamento, COUNT(*) AS cancelled
      FROM atendimentos
      WHERE status = 'CANCELADO'
        AND cancelado_em >= :start AND cancelado_em < :end
        AND estabelecimento_id = :tenant
      GROUP BY motivo_cancelamento
      ORDER BY COUNT(*) DESC, motivo_cancelamento
      """;

  private static final String SALES_RECEIVED_SQL =
      """
      SELECT COALESCE(SUM(p.valor), 0)
      FROM venda_pagamentos p
      JOIN vendas v ON v.id = p.venda_id
      WHERE p.recebido_em >= :start AND p.recebido_em < :end
        AND v.estabelecimento_id = :tenant
      """;

  private static final String REVERSED_COMMISSIONS_SQL =
      """
      SELECT COALESCE(SUM(comissao_estornada), 0) FROM estornos_atendimento
      WHERE criado_em >= :start AND criado_em < :end
        AND estabelecimento_id = :tenant
      """;

  private static final String ORDER_REFUND_REASONS_SQL =
      """
      SELECT motivo, COUNT(*) AS refunds, SUM(valor) AS amount
      FROM estornos_atendimento
      WHERE criado_em >= :start AND criado_em < :end
        AND estabelecimento_id = :tenant
      GROUP BY motivo
      ORDER BY COUNT(*) DESC, motivo
      """;

  private static final String SALE_REFUND_REASONS_SQL =
      """
      SELECT motivo, COUNT(*) AS refunds, SUM(valor) AS amount
      FROM venda_estornos
      WHERE criado_em >= :start AND criado_em < :end
        AND estabelecimento_id = :tenant
      GROUP BY motivo
      ORDER BY COUNT(*) DESC, motivo
      """;

  private final Jdbi jdbi;

  public OperationsReportJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public OperationsReport load(LocalDate from, LocalDate to) {
    return loadOperationsReport(from, to);
  }

  public OperationsReport loadOperationsReport(LocalDate from, LocalDate to) {
    Instant start = from.atStartOfDay(STORE_ZONE).toInstant();
    Instant end = to.plusDays(1).atStartOfDay(STORE_ZONE).toInstant();
    return jdbi.withHandle(
        handle -> {
          List<ReportOrder> orders = paidOrders(handle, start, end);
          Pauses pauses = pauses(handle, orders);
          Totals totals = totals(orders, pauses.durations());
          List<PauseBreakdown> pauseBreakdowns = new ArrayList<>();
          pauses
              .counts()
              .forEach((reason, count) -> pauseBreakdowns.add(new PauseBreakdown(reason, count)));
          List<RefundBreakdown> refundReasons = orderRefundReasons(handle, start, end);
          List<RefundBreakdown> saleRefundReasons = saleRefundReasons(handle, start, end);
          List<ReasonCount> cancellationReasons = cancellationReasons(handle, start, end);
          BigDecimal salesReceived = salesReceived(handle, start, end);
          BigDecimal reversedCommissions = reversedCommissions(handle, start, end);
          BigDecimal refunds = sum(refundReasons);
          BigDecimal saleRefunds = sum(saleRefundReasons);
          return new OperationsReport(
              from,
              to,
              orders.size(),
              totals.revenue(),
              totals.commissions(),
              totals.averageWashingMinutes(),
              totals.services(),
              totals.payments(),
              pauseBreakdowns,
              refundReasons.stream().mapToInt(RefundBreakdown::count).sum(),
              refunds,
              reversedCommissions,
              totals.revenue().subtract(refunds),
              totals.commissions().subtract(reversedCommissions),
              refundReasons,
              cancellationReasons.stream().mapToInt(ReasonCount::count).sum(),
              cancellationReasons,
              salesReceived,
              saleRefunds,
              salesReceived.subtract(saleRefunds),
              saleRefundReasons);
        });
  }

  /** Orders paid in the period, with their service, payment, commission and washing times. */
  private static List<ReportOrder> paidOrders(Handle handle, Instant start, Instant end) {
    return handle
        .createQuery(PAID_ORDERS_SQL)
        .bind(START, start)
        .bind(END, end)
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
  }

  /** Paused time per order (to discount from washing time) and pause counts per reason. */
  private static Pauses pauses(Handle handle, List<ReportOrder> orders) {
    Map<UUID, Duration> durations = new LinkedHashMap<>();
    Map<String, Integer> counts = new LinkedHashMap<>();
    if (orders.isEmpty()) {
      return new Pauses(durations, counts);
    }
    handle
        .createQuery(PAUSES_SQL)
        .bindList("orderIds", orders.stream().map(ReportOrder::id).toList())
        .map(
            (rs, ctx) -> {
              UUID orderId = rs.getObject("atendimento_id", UUID.class);
              Instant startedAt = rs.getTimestamp("iniciada_em").toInstant();
              java.sql.Timestamp resumed = rs.getTimestamp("retomada_em");
              Instant endedAt = resumed == null ? Instant.now() : resumed.toInstant();
              durations.merge(orderId, Duration.between(startedAt, endedAt), Duration::plus);
              counts.merge(rs.getString("motivo"), 1, Integer::sum);
              return orderId;
            })
        .list();
    return new Pauses(durations, counts);
  }

  /** Revenue, commissions, average washing time (minus pauses) and the service/payment splits. */
  private static Totals totals(List<ReportOrder> orders, Map<UUID, Duration> pausedTime) {
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
        Duration paused = pausedTime.getOrDefault(order.id(), Duration.ZERO);
        totalWashingMinutes += Math.max(0, elapsed.minus(paused).toMinutes());
        timedOrders++;
      }
    }
    List<ServiceBreakdown> serviceBreakdowns = new ArrayList<>();
    services.forEach(
        (name, summary) ->
            serviceBreakdowns.add(
                new ServiceBreakdown(name, summary.orders, summary.revenue, summary.commission)));
    List<PaymentBreakdown> paymentBreakdowns = new ArrayList<>();
    payments.forEach(
        (method, summary) ->
            paymentBreakdowns.add(new PaymentBreakdown(method, summary.orders, summary.amount)));
    return new Totals(
        revenue,
        commissions,
        timedOrders == 0 ? null : totalWashingMinutes / timedOrders,
        serviceBreakdowns,
        paymentBreakdowns);
  }

  private static List<ReasonCount> cancellationReasons(Handle handle, Instant start, Instant end) {
    return handle
        .createQuery(CANCELLATIONS_SQL)
        .bind(START, start)
        .bind(END, end)
        .map(
            (rs, ctx) ->
                new ReasonCount(rs.getString("motivo_cancelamento"), rs.getInt("cancelled")))
        .list();
  }

  private static BigDecimal salesReceived(Handle handle, Instant start, Instant end) {
    return handle
        .createQuery(SALES_RECEIVED_SQL)
        .bind(START, start)
        .bind(END, end)
        .mapTo(BigDecimal.class)
        .one();
  }

  private static BigDecimal reversedCommissions(Handle handle, Instant start, Instant end) {
    return handle
        .createQuery(REVERSED_COMMISSIONS_SQL)
        .bind(START, start)
        .bind(END, end)
        .mapTo(BigDecimal.class)
        .one();
  }

  private record Pauses(Map<UUID, Duration> durations, Map<String, Integer> counts) {}

  private record Totals(
      BigDecimal revenue,
      BigDecimal commissions,
      Long averageWashingMinutes,
      List<ServiceBreakdown> services,
      List<PaymentBreakdown> payments) {}

  private static List<RefundBreakdown> orderRefundReasons(
      Handle handle, Instant start, Instant end) {
    return queryRefundReasons(handle, ORDER_REFUND_REASONS_SQL, start, end);
  }

  private static List<RefundBreakdown> saleRefundReasons(
      Handle handle, Instant start, Instant end) {
    return queryRefundReasons(handle, SALE_REFUND_REASONS_SQL, start, end);
  }

  private static List<RefundBreakdown> queryRefundReasons(
      Handle handle, String sql, Instant start, Instant end) {
    try (var query = handle.createQuery(sql)) {
      return query
          .bind(START, start)
          .bind(END, end)
          .map(
              (rs, ctx) ->
                  new RefundBreakdown(
                      rs.getString("motivo"), rs.getInt("refunds"), rs.getBigDecimal("amount")))
          .list();
    }
  }

  private static BigDecimal sum(List<RefundBreakdown> reasons) {
    return reasons.stream().map(RefundBreakdown::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
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

  private static Instant timestamp(ResultSet rs, String column) throws SQLException {
    java.sql.Timestamp value = rs.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }
}
