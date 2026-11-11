package com.washflow.infra.db.jdbi.serviceorder;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * The service order SELECT with display details and its row mapper, shared by the repositories that
 * read {@code atendimentos} (lifecycle, yard and agenda).
 */
final class ServiceOrderRows {

  // Every read joins the display data (plate, customer, service, washer) the yard and queue
  // screens show on each card - see ServiceOrder.Details.
  static final String SELECT_WITH_DETAILS =
      """
      SELECT a.id, a.veiculo_id, a.servico_preco_id,
             (SELECT STRING_AGG(CAST(ats.servico_preco_id AS VARCHAR), ',' ORDER BY ats.ordem)
              FROM atendimento_servicos ats
              WHERE ats.atendimento_id = a.id) AS service_price_ids,
             a.lavador_id, a.status,
             a.data_agendamento, a.criado_em, a.atualizado_em,
             (SELECT p.motivo FROM pausas_atendimento p
              WHERE p.atendimento_id = a.id AND p.retomada_em IS NULL) AS pause_reason,
             a.previsao_entrega, a.observacoes,
             v.placa AS vehicle_plate, v.modelo AS vehicle_model, v.cor AS vehicle_color,
             COALESCE(c.nome, v.contato_nome) AS customer_name,
             COALESCE(c.telefone, v.contato_telefone) AS customer_phone,
             COALESCE((SELECT STRING_AGG(sp.nome, ' + ' ORDER BY ats.ordem)
              FROM atendimento_servicos ats
              JOIN servicos_preco sp ON sp.id = ats.servico_preco_id
              WHERE ats.atendimento_id = a.id),
              (SELECT sp.nome FROM servicos_preco sp WHERE sp.id = a.servico_preco_id))
                AS service_name,
             COALESCE((SELECT SUM(sp.preco)
              FROM atendimento_servicos ats
              JOIN servicos_preco sp ON sp.id = ats.servico_preco_id
              WHERE ats.atendimento_id = a.id),
              (SELECT sp.preco FROM servicos_preco sp WHERE sp.id = a.servico_preco_id), 0)
                AS service_total,
             l.nome AS washer_name
      FROM atendimentos a
      LEFT JOIN veiculos v ON v.id = a.veiculo_id
      LEFT JOIN usuarios c ON c.id = v.cliente_id
      LEFT JOIN usuarios l ON l.id = a.lavador_id
      """;

  // Hourly capacity: cancelled orders don't take a slot.
  static final String COUNT_SCHEDULED_BETWEEN_SQL =
      "SELECT COUNT(*) FROM atendimentos WHERE estabelecimento_id = :tenant"
          + " AND data_agendamento >= :start AND data_agendamento < :end"
          + " AND status <> 'CANCELADO'";

  private ServiceOrderRows() {}

  @SuppressWarnings("java:S1172") // Required by JDBI's RowMapper method signature.
  static ServiceOrder map(ResultSet rs, StatementContext ctx) throws SQLException {
    String servicePriceIds = rs.getString("service_price_ids");
    return new ServiceOrder(
        (UUID) rs.getObject("id"),
        (UUID) rs.getObject("veiculo_id"),
        rs.getInt("servico_preco_id"),
        (UUID) rs.getObject("lavador_id"),
        ServiceOrderStatus.fromDbValue(rs.getString("status")),
        rs.getTimestamp("data_agendamento").toInstant(),
        rs.getTimestamp("criado_em").toInstant(),
        rs.getTimestamp("atualizado_em").toInstant(),
        rs.getString("pause_reason"),
        timestamp(rs, "previsao_entrega"),
        rs.getString("observacoes"),
        new ServiceOrder.Details(
            rs.getString("vehicle_plate"),
            rs.getString("vehicle_model"),
            rs.getString("vehicle_color"),
            rs.getString("customer_name"),
            rs.getString("customer_phone"),
            rs.getString("service_name"),
            rs.getString("washer_name"),
            rs.getBigDecimal("service_total")),
        servicePriceIds == null
            ? java.util.List.of(rs.getInt("servico_preco_id"))
            : java.util.Arrays.stream(servicePriceIds.split(",")).map(Integer::parseInt).toList());
  }

  static Instant timestamp(ResultSet rs, String column) throws SQLException {
    java.sql.Timestamp value = rs.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }
}
