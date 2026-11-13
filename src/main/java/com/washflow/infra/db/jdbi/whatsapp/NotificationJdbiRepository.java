package com.washflow.infra.db.jdbi.whatsapp;

import com.washflow.data.protocols.db.whatsapp.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

public class NotificationJdbiRepository implements NotificationRepository {

  // Registered owner first, counter contact as fallback (see Vehicle.contactName).
  private static final String RECIPIENT_SQL =
      """
      SELECT COALESCE(c.telefone, v.contato_telefone) AS phone,
             COALESCE(c.nome, v.contato_nome) AS name,
             v.modelo, v.placa, e.nome AS establishment, a.data_agendamento
      FROM atendimentos a
      JOIN veiculos v ON v.id = a.veiculo_id
      LEFT JOIN usuarios c ON c.id = v.cliente_id
      JOIN estabelecimentos e ON e.id = a.estabelecimento_id
      WHERE a.id = :id AND a.estabelecimento_id = :tenant
      """;

  // Every lava-jato at once: the reminder job runs outside a request, so there is no tenant.
  private static final String DUE_REMINDERS_SQL =
      """
      SELECT a.id, a.estabelecimento_id
      FROM atendimentos a
      JOIN estabelecimentos e ON e.id = a.estabelecimento_id AND e.ativo
      WHERE a.status = 'AGENDADO'
        AND a.lembrete_enviado_em IS NULL
        AND a.data_agendamento > :from AND a.data_agendamento <= :to
        AND a.criado_em <= a.data_agendamento - INTERVAL '2 hours'
      ORDER BY a.data_agendamento
      """;

  private static final String MARK_REMINDED_SQL =
      """
      UPDATE atendimentos SET lembrete_enviado_em = :at
      WHERE id = :id AND estabelecimento_id = :tenant
        AND status = 'AGENDADO' AND lembrete_enviado_em IS NULL
      """;

  private final Jdbi jdbi;

  public NotificationJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Optional<Recipient> recipientOf(UUID serviceOrderId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(RECIPIENT_SQL)
                .bind("id", serviceOrderId)
                .map(
                    (rs, ctx) ->
                        new Recipient(
                            rs.getString("phone"),
                            rs.getString("name"),
                            rs.getString("modelo"),
                            rs.getString("placa"),
                            rs.getString("establishment"),
                            rs.getTimestamp("data_agendamento").toInstant()))
                .findOne()
                .filter(recipient -> recipient.phone() != null && !recipient.phone().isBlank()));
  }

  @Override
  public List<DueReminder> dueReminders(Instant from, Instant to) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(DUE_REMINDERS_SQL)
                .bind("from", from)
                .bind("to", to)
                .map(
                    (rs, ctx) ->
                        new DueReminder(
                            rs.getObject("id", UUID.class),
                            rs.getObject("estabelecimento_id", UUID.class)))
                .list());
  }

  @Override
  public boolean markReminded(UUID serviceOrderId, Instant at) {
    return jdbi.withHandle(
            handle ->
                handle
                    .createUpdate(MARK_REMINDED_SQL)
                    .bind("id", serviceOrderId)
                    .bind("at", at)
                    .execute())
        > 0;
  }
}
