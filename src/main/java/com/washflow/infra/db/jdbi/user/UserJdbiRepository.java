package com.washflow.infra.db.jdbi.user;

import com.washflow.data.protocols.db.user.CreateUserRepository;
import com.washflow.data.protocols.db.user.CreateWasherRepository;
import com.washflow.data.protocols.db.user.LoadAllUsersRepository;
import com.washflow.data.protocols.db.user.LoadUserByEmailRepository;
import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.data.protocols.db.user.LoadUserByPhoneRepository;
import com.washflow.data.protocols.db.user.LoadUserPasswordHashRepository;
import com.washflow.data.protocols.db.user.SaveUserPasswordHashRepository;
import com.washflow.data.protocols.db.user.UpdateUserActiveRepository;
import com.washflow.data.protocols.db.user.UpdateUserEmailRepository;
import com.washflow.data.protocols.db.user.UpdateUserProfileRepository;
import com.washflow.data.tenancy.TenantContext;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.user.UserHasActiveWashesError;
import com.washflow.infra.db.jdbi.UniqueViolations;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;

public class UserJdbiRepository
    implements CreateUserRepository,
        CreateWasherRepository,
        LoadAllUsersRepository,
        LoadUserByIdRepository,
        LoadUserByPhoneRepository,
        LoadUserPasswordHashRepository,
        SaveUserPasswordHashRepository,
        UpdateUserActiveRepository,
        UpdateUserProfileRepository,
        LoadUserByEmailRepository,
        UpdateUserEmailRepository {

  private static final String PHONE = "phone";
  private static final String EMAIL = "email";
  private static final String WHERE_ID = " WHERE id = :id";

  // Column name (Portuguese, matches usuarios.perfil) vs. bind parameter name (English, matches
  // this class's other :name/:phone/:createdAt placeholders) - two different strings, unlike
  // ServiceOrderJdbiRepository's STATUS constant where the column and bind name happen to match.
  private static final String PROFILE_COLUMN = "perfil";
  private static final String PROFILE_PARAM = "profile";

  private static final String SELECT_SQL =
      "SELECT id, nome, telefone, email, perfil, criado_em, ativo FROM usuarios";

  private static final String IN_TENANT = " AND estabelecimento_id = :tenant";
  private static final String ACTIVE_ESTABLISHMENT =
      " AND estabelecimento_id IN (SELECT id FROM estabelecimentos WHERE ativo = TRUE)";
  private static final String UPDATE_PROFILE_SQL =
      "UPDATE usuarios SET perfil = :profile WHERE id = :id" + IN_TENANT;
  private static final String UPDATE_PASSWORD_HASH_SQL =
      "UPDATE usuarios SET senha_hash = :passwordHash WHERE id = :id";
  private static final String UPDATE_ACTIVE_SQL =
      "UPDATE usuarios SET ativo = :active WHERE id = :id" + IN_TENANT;

  private final Jdbi jdbi;

  public UserJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public User create(User user, String passwordHash) {
    UniqueViolations.translate(
        () ->
            jdbi.useHandle(
                handle ->
                    handle
                        .createUpdate(
                            """
                    INSERT INTO usuarios
                      (id, estabelecimento_id, nome, telefone, email, senha_hash, perfil,
                       criado_em, ativo)
                    VALUES
                      (:id, :tenant, :name, :phone, :email, :passwordHash, :profile,
                       :createdAt, :active)
                    """)
                        .bind("id", user.id())
                        .bind("name", user.name())
                        .bind(PHONE, user.phone())
                        .bind(EMAIL, user.email())
                        .bind("passwordHash", passwordHash)
                        .bind(PROFILE_PARAM, user.profile().dbValue())
                        .bind("createdAt", user.createdAt())
                        .bind("active", user.active())
                        .execute()));

    return user;
  }

  @Override
  public Optional<User> loadById(UUID id) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_SQL + WHERE_ID + tenantScope())
                .bind("id", id)
                .map(this::mapRow)
                .findOne());
  }

  /**
   * Inside an establishment: that establishment's account with this phone. Without one (washer PIN
   * login on a shared device): the staff account with this phone, only when exactly one exists -
   * customers always pick their lava-jato first.
   */
  @Override
  public Optional<User> loadByPhone(String phone) {
    if (TenantContext.current().isPresent()) {
      return jdbi.withHandle(
          handle ->
              handle
                  .createQuery(SELECT_SQL + " WHERE telefone = :phone" + IN_TENANT)
                  .bind(PHONE, phone)
                  .map(this::mapRow)
                  .findOne());
    }
    List<User> staff =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        SELECT_SQL
                            + " WHERE telefone = :phone AND perfil IN ('LAVADOR', 'GERENTE')"
                            + ACTIVE_ESTABLISHMENT)
                    .bind(PHONE, phone)
                    .map(this::mapRow)
                    .list());
    return staff.size() == 1 ? Optional.of(staff.get(0)) : Optional.empty();
  }

  private static String tenantScope() {
    return TenantContext.current().isPresent() ? IN_TENANT : "";
  }

  @Override
  public java.util.List<User> loadAll() {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    SELECT_SQL + " WHERE estabelecimento_id = :tenant ORDER BY nome, telefone")
                .map(this::mapRow)
                .list());
  }

  @Override
  public User createWasher(User user, String passwordHash) {
    return create(user, passwordHash);
  }

  @Override
  public User updateActive(UUID id, boolean active) {
    return jdbi.inTransaction(
        handle -> {
          if (!active) {
            String profile =
                handle
                    .createQuery("SELECT perfil FROM usuarios WHERE id = :id" + IN_TENANT)
                    .bind("id", id)
                    .mapTo(String.class)
                    .findOne()
                    .orElse(null);
            if (UserProfile.WASHER.dbValue().equals(profile)) {
              boolean hasActiveWashes =
                  handle
                          .createQuery(
                              """
                              SELECT COUNT(*) FROM atendimentos
                              WHERE lavador_id = :userId AND status IN ('EM_LAVAGEM', 'PAUSADA')
                              """)
                          .bind("userId", id)
                          .mapTo(Integer.class)
                          .one()
                      > 0;
              if (hasActiveWashes) {
                throw new UserHasActiveWashesError(id);
              }
            }
          }
          handle.createUpdate(UPDATE_ACTIVE_SQL).bind("id", id).bind("active", active).execute();
          return handle.createQuery(SELECT_SQL + WHERE_ID).bind("id", id).map(this::mapRow).one();
        });
  }

  @Override
  public Optional<String> loadPasswordHash(java.util.UUID userId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT senha_hash FROM usuarios WHERE id = :id" + tenantScope())
                .bind("id", userId)
                .mapTo(String.class)
                .findOne());
  }

  @Override
  public void savePasswordHash(java.util.UUID userId, String passwordHash) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(UPDATE_PASSWORD_HASH_SQL)
                .bind("id", userId)
                .bind("passwordHash", passwordHash)
                .execute());
  }

  @Override
  public User updateProfile(UUID id, UserProfile profile) {
    return jdbi.inTransaction(
        handle -> {
          if (profile != UserProfile.WASHER) {
            boolean hasActiveWashes =
                handle
                        .createQuery(
                            """
                            SELECT COUNT(*) FROM atendimentos
                            WHERE lavador_id = :userId AND status IN ('EM_LAVAGEM', 'PAUSADA')
                            """)
                        .bind("userId", id)
                        .mapTo(Integer.class)
                        .one()
                    > 0;
            if (hasActiveWashes) {
              throw new UserHasActiveWashesError(id);
            }
          }
          handle
              .createUpdate(UPDATE_PROFILE_SQL)
              .bind("id", id)
              .bind(PROFILE_PARAM, profile.dbValue())
              .execute();

          return handle.createQuery(SELECT_SQL + WHERE_ID).bind("id", id).map(this::mapRow).one();
        });
  }

  private User mapRow(ResultSet rs, StatementContext ctx) throws SQLException {
    return new User(
        (UUID) rs.getObject("id"),
        rs.getString("nome"),
        rs.getString("telefone"),
        UserProfile.fromDbValue(rs.getString(PROFILE_COLUMN)),
        rs.getTimestamp("criado_em").toInstant(),
        rs.getBoolean("ativo"),
        rs.getString(EMAIL));
  }

  @Override
  public Optional<User> loadByEmail(String email) {
    return jdbi.withHandle(
        handle ->
            handle
                // E-mail login is global (it says which lava-jato); a suspended one can't sign in.
                .createQuery(SELECT_SQL + " WHERE email = :email" + ACTIVE_ESTABLISHMENT)
                .bind(EMAIL, email)
                .map(this::mapRow)
                .findOne());
  }

  @Override
  public User updateEmail(UUID userId, String email) {
    return jdbi.inTransaction(
        handle -> {
          handle
              .createUpdate("UPDATE usuarios SET email = :email WHERE id = :id" + IN_TENANT)
              .bind("id", userId)
              .bind(EMAIL, email)
              .execute();
          return handle
              .createQuery(SELECT_SQL + WHERE_ID)
              .bind("id", userId)
              .map(this::mapRow)
              .one();
        });
  }
}
