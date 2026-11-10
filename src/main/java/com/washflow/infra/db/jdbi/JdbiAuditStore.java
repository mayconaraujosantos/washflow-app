package com.washflow.infra.db.jdbi;

import com.washflow.application.audit.AuditStore;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

/**
 * {@code auditoria} (V4). Inserts bind the establishment explicitly (the platform admin has none);
 * reads go through {@code :tenant}, so a manager only sees their own lava-jato.
 */
public class JdbiAuditStore implements AuditStore {

  private static final String SAVE_INSERT_SQL =
      """
      INSERT INTO auditoria
        (estabelecimento_id, usuario_id, perfil, metodo, rota, caminho, status,
         detalhes, criado_em)
      VALUES (CAST(:establishmentId AS UUID), :userId, :profile, :method, :route,
              :path, :status, :details, :at)
      """;

  private static final String LIST_SELECT_SQL =
      """
      SELECT a.criado_em, a.usuario_id, u.nome, a.perfil, a.metodo, a.rota,
             a.caminho, a.status, a.detalhes
      FROM auditoria a
      LEFT JOIN usuarios u ON u.id = a.usuario_id
      WHERE a.estabelecimento_id = :tenant
        AND a.criado_em >= :start AND a.criado_em < :end
      ORDER BY a.criado_em DESC
      """;

  private static final ZoneId STORE_ZONE = ZoneId.of("America/Manaus");

  private final Jdbi jdbi;

  public JdbiAuditStore(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public void save(Entry entry) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(SAVE_INSERT_SQL)
                .bind("establishmentId", entry.establishmentId())
                .bind("userId", entry.userId())
                .bind("profile", entry.profile())
                .bind("method", entry.method())
                .bind("route", entry.route())
                .bind("path", entry.path())
                .bind("status", entry.status())
                .bind("details", entry.details())
                .bind("at", entry.at())
                .execute());
  }

  @Override
  public List<Row> list(LocalDate from, LocalDate to) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(LIST_SELECT_SQL)
                .bind("start", from.atStartOfDay(STORE_ZONE).toInstant())
                .bind("end", to.plusDays(1).atStartOfDay(STORE_ZONE).toInstant())
                .map(
                    (rs, ctx) ->
                        new Row(
                            rs.getTimestamp("criado_em").toInstant(),
                            rs.getObject("usuario_id", UUID.class),
                            rs.getString("nome"),
                            rs.getString("perfil"),
                            rs.getString("metodo"),
                            rs.getString("rota"),
                            rs.getString("caminho"),
                            rs.getInt("status"),
                            rs.getString("detalhes")))
                .list());
  }
}
