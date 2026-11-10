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
      SELECT a.id, a.veiculo_id, a.servico_preco_id, a.lavador_id, a.status,
             a.data_agendamento, a.criado_em, a.atualizado_em,
             (SELECT p.motivo FROM pausas_atendimento p
              WHERE p.atendimento_id = a.id AND p.retomada_em IS NULL) AS pause_reason,
             a.previsao_entrega, a.observacoes,
             v.placa AS vehicle_plate, v.modelo AS vehicle_model, v.cor AS vehicle_color,
             COALESCE(c.nome, v.contato_nome) AS customer_name,
             COALESCE(c.telefone, v.contato_telefone) AS customer_phone,
             s.nome AS service_name, l.nome AS washer_name
      FROM atendimentos a
      LEFT JOIN veiculos v ON v.id = a.veiculo_id
      LEFT JOIN usuarios c ON c.id = v.cliente_id
      LEFT JOIN servicos_preco s ON s.id = a.servico_preco_id
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
            rs.getString("washer_name")));
  }

  static Instant timestamp(ResultSet rs, String column) throws SQLException {
    java.sql.Timestamp value = rs.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }
}
