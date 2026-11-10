package com.washflow.infra.db.jdbi;

import com.washflow.application.idempotency.IdempotencyStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

/**
 * {@code chaves_idempotencia} (V3). Rows are keyed by user + key, so they never use {@code :tenant}
 * (the platform admin has no lava-jato); {@code estabelecimento_id} is kept for auditing.
 */
public class JdbiIdempotencyStore implements IdempotencyStore {

  private static final String CLAIM_INSERT_SQL =
      """
      INSERT INTO chaves_idempotencia
        (usuario_id, chave, estabelecimento_id, metodo, rota, hash_requisicao,
         criado_em)
      VALUES (:userId, :key, CAST(:establishmentId AS UUID), :method, :route, :hash, :now)
      ON CONFLICT (usuario_id, chave) DO NOTHING
      """;

  private static final String CLAIM_UPDATE_SQL =
      """
      UPDATE chaves_idempotencia SET criado_em = :now
      WHERE usuario_id = :userId AND chave = :key AND status_resposta IS NULL
        AND hash_requisicao = :hash AND criado_em < :stale
      """;

  private static final String CLAIM_SELECT_SQL =
      """
      SELECT hash_requisicao, status_resposta, corpo_resposta
      FROM chaves_idempotencia WHERE usuario_id = :userId AND chave = :key
      """;

  private static final String COMPLETE_UPDATE_SQL =
      """
      UPDATE chaves_idempotencia
      SET status_resposta = :status, corpo_resposta = :body
      WHERE usuario_id = :userId AND chave = :key
      """;

  private static final String RELEASE_DELETE_SQL =
      """
      DELETE FROM chaves_idempotencia
      WHERE usuario_id = :userId AND chave = :key AND status_resposta IS NULL
      """;

  private static final String USER_ID = "userId";

  static final Duration RETENTION = Duration.ofHours(24);
  // A request that never completed nor released its key (server killed mid-request).
  static final Duration STALE_AFTER = Duration.ofMinutes(2);

  private final Jdbi jdbi;

  public JdbiIdempotencyStore(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Claim claim(
      UUID userId,
      String key,
      UUID establishmentId,
      String method,
      String route,
      String requestHash,
      Instant now) {
    return jdbi.inTransaction(
        handle -> {
          handle
              .createUpdate("DELETE FROM chaves_idempotencia WHERE criado_em < :expired")
              .bind("expired", now.minus(RETENTION))
              .execute();
          int inserted =
              handle
                  .createUpdate(CLAIM_INSERT_SQL)
                  .bind(USER_ID, userId)
                  .bind("key", key)
                  .bind("establishmentId", establishmentId)
                  .bind("method", method)
                  .bind("route", route)
                  .bind("hash", requestHash)
                  .bind("now", now)
                  .execute();
          if (inserted == 1) {
            return new Acquired();
          }
          int takenOver =
              handle
                  .createUpdate(CLAIM_UPDATE_SQL)
                  .bind(USER_ID, userId)
                  .bind("key", key)
                  .bind("hash", requestHash)
                  .bind("now", now)
                  .bind("stale", now.minus(STALE_AFTER))
                  .execute();
          if (takenOver == 1) {
            return new Acquired();
          }
          Optional<Claim> existing =
              handle
                  .createQuery(CLAIM_SELECT_SQL)
                  .bind(USER_ID, userId)
                  .bind("key", key)
                  .map(
                      (rs, ctx) -> {
                        String hash = rs.getString("hash_requisicao");
                        int status = rs.getInt("status_resposta");
                        return rs.wasNull()
                            ? (Claim) new InProgress(hash)
                            : new Replay(hash, status, rs.getString("corpo_resposta"));
                      })
                  .findOne();
          // Released between the insert and this read: the caller may simply run it.
          return existing.orElseGet(Acquired::new);
        });
  }

  @Override
  public void complete(UUID userId, String key, int status, String body) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(COMPLETE_UPDATE_SQL)
                .bind("status", status)
                .bind("body", body)
                .bind(USER_ID, userId)
                .bind("key", key)
                .execute());
  }

  @Override
  public void release(UUID userId, String key) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(RELEASE_DELETE_SQL)
                .bind(USER_ID, userId)
                .bind("key", key)
                .execute());
  }
}
