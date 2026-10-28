package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.CreateUserRepository;
import com.washflow.data.protocols.db.LoadUserByIdRepository;
import com.washflow.data.protocols.db.LoadUserByPhoneRepository;
import com.washflow.data.protocols.db.LoadUserPasswordHashRepository;
import com.washflow.data.protocols.db.SaveUserPasswordHashRepository;
import com.washflow.data.protocols.db.UpdateUserProfileRepository;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.UserHasActiveWashesError;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;

public class UserJdbiRepository
    implements CreateUserRepository,
        LoadUserByIdRepository,
        LoadUserByPhoneRepository,
        LoadUserPasswordHashRepository,
        SaveUserPasswordHashRepository,
        UpdateUserProfileRepository {

  // Column name (Portuguese, matches usuarios.perfil) vs. bind parameter name (English, matches
  // this class's other :name/:phone/:createdAt placeholders) - two different strings, unlike
  // ServiceOrderJdbiRepository's STATUS constant where the column and bind name happen to match.
  private static final String PROFILE_COLUMN = "perfil";
  private static final String PROFILE_PARAM = "profile";

  private static final String SELECT_SQL =
      "SELECT id, nome, telefone, perfil, criado_em, ativo FROM usuarios";

  private static final String UPDATE_PROFILE_SQL =
      "UPDATE usuarios SET perfil = :profile WHERE id = :id";
  private static final String UPDATE_PASSWORD_HASH_SQL =
      "UPDATE usuarios SET senha_hash = :passwordHash WHERE id = :id";
  private static final String UPDATE_ACTIVE_SQL =
      "UPDATE usuarios SET ativo = :active WHERE id = :id";

  private final Jdbi jdbi;

  public UserJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public User create(User user, String passwordHash) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                    INSERT INTO usuarios (id, nome, telefone, senha_hash, perfil, criado_em, ativo)
                    VALUES (:id, :name, :phone, :passwordHash, :profile, :createdAt, :active)
                    """)
                .bind("id", user.id())
                .bind("name", user.name())
                .bind("phone", user.phone())
                .bind("passwordHash", passwordHash)
                .bind(PROFILE_PARAM, user.profile().dbValue())
                .bind("createdAt", user.createdAt())
                .bind("active", user.active())
                .execute());

    return user;
  }

  @Override
  public Optional<User> loadById(UUID id) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_SQL + " WHERE id = :id")
                .bind("id", id)
                .map(this::mapRow)
                .findOne());
  }

  @Override
  public Optional<User> loadByPhone(String phone) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_SQL + " WHERE telefone = :phone")
                .bind("phone", phone)
                .map(this::mapRow)
                .findOne());
  }

  public java.util.List<User> loadAll() {
    return jdbi.withHandle(
        handle ->
            handle.createQuery(SELECT_SQL + " ORDER BY nome, telefone").map(this::mapRow).list());
  }

  public boolean updateActive(UUID id, boolean active) {
    return jdbi.inTransaction(
        handle -> {
          if (!active) {
            String profile =
                handle
                    .createQuery("SELECT perfil FROM usuarios WHERE id = :id")
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
          return handle
                  .createUpdate(UPDATE_ACTIVE_SQL)
                  .bind("id", id)
                  .bind("active", active)
                  .execute()
              == 1;
        });
  }

  @Override
  public Optional<String> loadPasswordHash(java.util.UUID userId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT senha_hash FROM usuarios WHERE id = :id")
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

          return handle
              .createQuery(SELECT_SQL + " WHERE id = :id")
              .bind("id", id)
              .map(this::mapRow)
              .one();
        });
  }

  private User mapRow(ResultSet rs, StatementContext ctx) throws SQLException {
    return new User(
        (UUID) rs.getObject("id"),
        rs.getString("nome"),
        rs.getString("telefone"),
        UserProfile.fromDbValue(rs.getString(PROFILE_COLUMN)),
        rs.getTimestamp("criado_em").toInstant(),
        rs.getBoolean("ativo"));
  }
}
