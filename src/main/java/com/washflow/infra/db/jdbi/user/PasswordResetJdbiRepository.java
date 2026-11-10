package com.washflow.infra.db.jdbi.user;

import com.washflow.data.protocols.db.user.PasswordResetRepository;
import com.washflow.data.tenancy.TenantContext;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

public class PasswordResetJdbiRepository implements PasswordResetRepository {

  private static final String USER_ID = "userId";
  private static final String IN_TENANT = " AND estabelecimento_id = :tenant";
  private static final String LOAD_RECOVERY_EMAIL_SQL =
      "SELECT COALESCE(email_recuperacao, email) FROM usuarios WHERE id = :userId";
  private static final String SAVE_RECOVERY_EMAIL_SQL =
      "UPDATE usuarios SET email_recuperacao = :email WHERE id = :userId";
  private static final String SAVE_CODE_SQL =
      """
      INSERT INTO codigos_senha (usuario_id, codigo_hash, tentativas, expira_em, criado_em)
      VALUES (:userId, :hash, 0, :expiresAt, :createdAt)
      ON CONFLICT (usuario_id) DO UPDATE
        SET codigo_hash = EXCLUDED.codigo_hash, tentativas = 0,
            expira_em = EXCLUDED.expira_em, criado_em = EXCLUDED.criado_em
      """;
  private static final String LOAD_CODE_SQL =
      "SELECT codigo_hash, tentativas, expira_em, criado_em FROM codigos_senha"
          + " WHERE usuario_id = :userId";
  private static final String INCREMENT_ATTEMPTS_SQL =
      "UPDATE codigos_senha SET tentativas = tentativas + 1 WHERE usuario_id = :userId";
  private static final String DELETE_CODE_SQL =
      "DELETE FROM codigos_senha WHERE usuario_id = :userId";

  private final Jdbi jdbi;

  public PasswordResetJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  /** Inside a lava-jato the account must belong to it; staff requests run without one. */
  private static String tenantScope() {
    return TenantContext.current().isPresent() ? IN_TENANT : "";
  }

  @Override
  public Optional<String> loadRecoveryEmail(UUID userId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(LOAD_RECOVERY_EMAIL_SQL + tenantScope())
                .bind(USER_ID, userId)
                .mapTo(String.class)
                .findOne()
                .filter(email -> !email.isBlank()));
  }

  @Override
  public void saveRecoveryEmail(UUID userId, String email) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(SAVE_RECOVERY_EMAIL_SQL + tenantScope())
                .bind("email", email)
                .bind(USER_ID, userId)
                .execute());
  }

  @Override
  public void saveCode(UUID userId, String codeHash, Instant expiresAt, Instant createdAt) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(SAVE_CODE_SQL)
                .bind(USER_ID, userId)
                .bind("hash", codeHash)
                .bind("expiresAt", expiresAt)
                .bind("createdAt", createdAt)
                .execute());
  }

  @Override
  public Optional<PendingCode> loadCode(UUID userId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(LOAD_CODE_SQL)
                .bind(USER_ID, userId)
                .map(
                    (rs, ctx) ->
                        new PendingCode(
                            rs.getString("codigo_hash"),
                            rs.getInt("tentativas"),
                            rs.getTimestamp("expira_em").toInstant(),
                            rs.getTimestamp("criado_em").toInstant()))
                .findOne());
  }

  @Override
  public void incrementAttempts(UUID userId) {
    jdbi.useHandle(
        handle -> handle.createUpdate(INCREMENT_ATTEMPTS_SQL).bind(USER_ID, userId).execute());
  }

  @Override
  public void deleteCode(UUID userId) {
    jdbi.useHandle(handle -> handle.createUpdate(DELETE_CODE_SQL).bind(USER_ID, userId).execute());
  }
}
