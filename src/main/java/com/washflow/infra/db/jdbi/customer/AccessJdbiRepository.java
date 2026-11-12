package com.washflow.infra.db.jdbi.customer;

import com.washflow.data.protocols.db.customer.CustomerIdentityRepository;
import com.washflow.data.protocols.db.customer.OtpRepository;
import com.washflow.data.protocols.db.customer.TicketRepository;
import com.washflow.data.protocols.db.user.StaffCredentialsRepository;
import com.washflow.domain.entities.customer.OtpRecipient;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.domain.entities.serviceorder.TicketView;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

/** Persistence for badges/PINs, tickets, access codes and passwordless customers. */
public class AccessJdbiRepository
    implements StaffCredentialsRepository,
        TicketRepository,
        OtpRepository,
        CustomerIdentityRepository {

  private static final String AND_TENANT = " AND estabelecimento_id = :tenant";
  private static final String PHONE = "phone";
  private static final String USER_COLUMNS = "id, nome, telefone, email, perfil, criado_em, ativo";
  private static final String ADDRESS = "address";
  private static final String EMAIL = "email";

  private static final String SAVE_BADGE_UPDATE_SQL =
      """
      UPDATE usuarios
      SET pin_hash = :pinHash, cracha_hash = :badgeHash,
          pin_tentativas = 0, pin_bloqueado_ate = NULL
      WHERE id = :id AND estabelecimento_id = :tenant
      """;

  private static final String RECORD_PIN_FAILURE_UPDATE_SQL =
      """
      UPDATE usuarios
      SET pin_tentativas = :attempts, pin_bloqueado_ate = :lockedUntil
      WHERE id = :id
      """;

  private static final String ISSUE_UPDATE_SQL =
      """
      UPDATE atendimentos SET ticket_token = :token
      WHERE id = :id AND ticket_token IS NULL AND estabelecimento_id = :tenant
      """;

  private static final String LOAD_BY_TOKEN_SELECT_SQL =
      """
      SELECT e.nome AS establishment, v.placa, v.modelo,
             COALESCE((SELECT STRING_AGG(sp.nome, ' + ' ORDER BY ats.ordem)
              FROM atendimento_servicos ats
              JOIN servicos_preco sp ON sp.id = ats.servico_preco_id
              WHERE ats.atendimento_id = a.id),
              (SELECT sp.nome FROM servicos_preco sp WHERE sp.id = a.servico_preco_id))
                AS service,
             a.status, a.data_agendamento, a.previsao_entrega, a.atualizado_em,
             (SELECT p.motivo FROM pausas_atendimento p
              WHERE p.atendimento_id = a.id AND p.retomada_em IS NULL) AS pause_reason
      FROM atendimentos a
      JOIN veiculos v ON v.id = a.veiculo_id
      JOIN estabelecimentos e ON e.id = a.estabelecimento_id
      WHERE a.ticket_token = :token
      """;

  private static final String SAVE_INSERT_SQL =
      """
      INSERT INTO codigos_acesso
        (id, estabelecimento_id, telefone, email, codigo_hash, tentativas, expira_em, criado_em)
      VALUES (:id, :tenant, :phone, :email, :hash, 0, :expiresAt, :createdAt)
      """;

  private final Jdbi jdbi;

  public AccessJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  // ------------------------------------------------------------ badges / PIN

  @Override
  public void saveBadge(UUID userId, String pinHash, String badgeHash) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(SAVE_BADGE_UPDATE_SQL)
                .bind("id", userId)
                .bind("pinHash", pinHash)
                .bind("badgeHash", badgeHash)
                .execute());
  }

  @Override
  public Optional<PinState> loadPinState(UUID userId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT pin_hash, pin_tentativas, pin_bloqueado_ate FROM usuarios WHERE id = :id")
                .bind("id", userId)
                .map(
                    (rs, ctx) ->
                        new PinState(
                            rs.getString("pin_hash"),
                            rs.getInt("pin_tentativas"),
                            instant(rs, "pin_bloqueado_ate")))
                .findOne());
  }

  @Override
  public void recordPinFailure(UUID userId, int failedAttempts, Instant lockedUntil) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(RECORD_PIN_FAILURE_UPDATE_SQL)
                .bind("id", userId)
                .bind("attempts", failedAttempts)
                .bind("lockedUntil", lockedUntil)
                .execute());
  }

  @Override
  public void resetPinFailures(UUID userId) {
    recordPinFailure(userId, 0, null);
  }

  @Override
  public Optional<User> loadByBadgeHash(String badgeHash) {
    // Badge tokens are random and unique across establishments: the badge says which one.
    return loadUser(
        "cracha_hash",
        badgeHash,
        " AND estabelecimento_id IN (SELECT id FROM estabelecimentos WHERE ativo = TRUE)");
  }

  // ------------------------------------------------------------ tickets

  @Override
  public Optional<String> issue(UUID serviceOrderId, String candidate) {
    return jdbi.inTransaction(
        handle -> {
          handle
              .createUpdate(ISSUE_UPDATE_SQL)
              .bind("id", serviceOrderId)
              .bind("token", candidate)
              .execute();
          return handle
              .createQuery("SELECT ticket_token FROM atendimentos WHERE id = :id" + AND_TENANT)
              .bind("id", serviceOrderId)
              .mapTo(String.class)
              .findOne();
        });
  }

  @Override
  public Optional<TicketView> loadByToken(String token) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(LOAD_BY_TOKEN_SELECT_SQL)
                .bind("token", token)
                .map(
                    (rs, ctx) ->
                        new TicketView(
                            rs.getString("establishment"),
                            rs.getString("placa"),
                            rs.getString("modelo"),
                            rs.getString("service"),
                            ServiceOrderStatus.fromDbValue(rs.getString("status")),
                            rs.getString("pause_reason"),
                            instant(rs, "data_agendamento"),
                            instant(rs, "previsao_entrega"),
                            instant(rs, "atualizado_em")))
                .findOne());
  }

  // ------------------------------------------------------------ access codes (WhatsApp / e-mail)

  @Override
  public void save(OtpRecipient recipient, String codeHash, Instant expiresAt, Instant createdAt) {
    jdbi.useTransaction(
        handle -> {
          handle
              .createUpdate("DELETE FROM codigos_acesso WHERE " + recipientFilter(recipient))
              .bind(ADDRESS, recipient.address())
              .execute();
          handle
              .createUpdate(SAVE_INSERT_SQL)
              .bind("id", UUID.randomUUID())
              .bind(PHONE, recipient.isEmail() ? null : recipient.address())
              .bind(EMAIL, recipient.isEmail() ? recipient.address() : null)
              .bind("hash", codeHash)
              .bind("expiresAt", expiresAt)
              .bind("createdAt", createdAt)
              .execute();
        });
  }

  @Override
  public Optional<PendingCode> loadPending(OtpRecipient recipient) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT id, codigo_hash, tentativas, expira_em, criado_em FROM codigos_acesso"
                        + " WHERE "
                        + recipientFilter(recipient)
                        + " ORDER BY criado_em DESC")
                .bind(ADDRESS, recipient.address())
                .map(
                    (rs, ctx) ->
                        new PendingCode(
                            rs.getObject("id", UUID.class),
                            rs.getString("codigo_hash"),
                            rs.getInt("tentativas"),
                            instant(rs, "expira_em"),
                            instant(rs, "criado_em")))
                .findFirst());
  }

  @Override
  public void incrementAttempts(UUID codeId) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    "UPDATE codigos_acesso SET tentativas = tentativas + 1 WHERE id = :id"
                        + AND_TENANT)
                .bind("id", codeId)
                .execute());
  }

  @Override
  public void delete(OtpRecipient recipient) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("DELETE FROM codigos_acesso WHERE " + recipientFilter(recipient))
                .bind(ADDRESS, recipient.address())
                .execute());
  }

  /** {@code email = :address} or {@code telefone = :address}, inside the current lava-jato. */
  private static String recipientFilter(OtpRecipient recipient) {
    return column(recipient) + " = :" + ADDRESS + AND_TENANT;
  }

  /** Fixed column per channel, never user input. */
  private static String column(OtpRecipient recipient) {
    return recipient.isEmail() ? EMAIL : "telefone";
  }

  // ------------------------------------------------------------ Google

  @Override
  public Optional<User> loadByGoogleSubject(String subject) {
    // Customer accounts belong to one lava-jato: the same Google account can sign up in several.
    return loadUser("google_sub", subject, AND_TENANT);
  }

  @Override
  public void linkGoogleSubject(UUID userId, String subject) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate("UPDATE usuarios SET google_sub = :sub WHERE id = :id" + AND_TENANT)
                .bind("id", userId)
                .bind("sub", subject)
                .execute());
  }

  @Override
  public Optional<User> loadByEmail(String email) {
    return loadUser(EMAIL, email, AND_TENANT);
  }

  // ------------------------------------------------------------ helpers

  private Optional<User> loadUser(String column, String value, String scope) {
    // column is one of the constants above, never user input.
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT "
                        + USER_COLUMNS
                        + " FROM usuarios WHERE "
                        + column
                        + " = :value"
                        + scope)
                .bind("value", value)
                .map(
                    (rs, ctx) ->
                        new User(
                            rs.getObject("id", UUID.class),
                            rs.getString("nome"),
                            rs.getString("telefone"),
                            UserProfile.fromDbValue(rs.getString("perfil")),
                            rs.getTimestamp("criado_em").toInstant(),
                            rs.getBoolean("ativo"),
                            rs.getString(EMAIL)))
                .findOne());
  }

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    Timestamp value = rs.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }
}
