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
                        AND a.estabelecimento_id = :tenant
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
          List<RefundBreakdown> refundReasons =
              refundReasons(handle, "estornos_atendimento", start, end);
          List<RefundBreakdown> saleRefundReasons =
              refundReasons(handle, "venda_estornos", start, end);
          List<ReasonCount> cancellationReasons =
              handle
                  .createQuery(
                      """
                      SELECT motivo_cancelamento, COUNT(*) AS cancelled
                      FROM atendimentos
                      WHERE status = 'CANCELADO'
                        AND cancelado_em >= :start AND cancelado_em < :end
                        AND estabelecimento_id = :tenant
                      GROUP BY motivo_cancelamento
                      ORDER BY COUNT(*) DESC, motivo_cancelamento
                      """)
                  .bind("start", start)
                  .bind("end", end)
                  .map(
                      (rs, ctx) ->
                          new ReasonCount(
                              rs.getString("motivo_cancelamento"), rs.getInt("cancelled")))
                  .list();
          BigDecimal salesReceived =
              handle
                  .createQuery(
                      """
                      SELECT COALESCE(SUM(p.valor), 0)
                      FROM venda_pagamentos p
                      JOIN vendas v ON v.id = p.venda_id
                      WHERE p.recebido_em >= :start AND p.recebido_em < :end
                        AND v.estabelecimento_id = :tenant
                      """)
                  .bind("start", start)
                  .bind("end", end)
                  .mapTo(BigDecimal.class)
                  .one();
          BigDecimal reversedCommissions =
              handle
                  .createQuery(
                      """
                      SELECT COALESCE(SUM(comissao_estornada), 0) FROM estornos_atendimento
                      WHERE criado_em >= :start AND criado_em < :end
                        AND estabelecimento_id = :tenant
                      """)
                  .bind("start", start)
                  .bind("end", end)
                  .mapTo(BigDecimal.class)
                  .one();
          BigDecimal refunds = sum(refundReasons);
          BigDecimal saleRefunds = sum(saleRefundReasons);
          return new OperationsReport(
              from,
              to,
              orders.size(),
              revenue,
              commissions,
              timedOrders == 0 ? null : totalWashingMinutes / timedOrders,
              serviceBreakdowns,
              paymentBreakdowns,
              pauseBreakdowns,
              refundReasons.stream().mapToInt(RefundBreakdown::count).sum(),
              refunds,
              reversedCommissions,
              revenue.subtract(refunds),
              commissions.subtract(reversedCommissions),
              refundReasons,
              cancellationReasons.stream().mapToInt(ReasonCount::count).sum(),
              cancellationReasons,
              salesReceived,
              saleRefunds,
              salesReceived.subtract(saleRefunds),
              saleRefundReasons);
        });
  }

  // Refunds by reason in the period (refund date): orders' or quick sales' refund table.
  private static List<RefundBreakdown> refundReasons(
      Handle handle, String table, Instant start, Instant end) {
    return handle
        .createQuery(
            "SELECT motivo, COUNT(*) AS refunds, SUM(valor) AS amount FROM "
                + table
                + " WHERE criado_em >= :start AND criado_em < :end"
                + " AND estabelecimento_id = :tenant"
                + " GROUP BY motivo ORDER BY COUNT(*) DESC, motivo")
        .bind("start", start)
        .bind("end", end)
        .map(
            (rs, ctx) ->
                new RefundBreakdown(
                    rs.getString("motivo"), rs.getInt("refunds"), rs.getBigDecimal("amount")))
        .list();
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
