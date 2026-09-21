package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.CreateUserRepository;
import com.washflow.data.protocols.db.LoadUserByIdRepository;
import com.washflow.data.protocols.db.LoadUserByPhoneRepository;
import com.washflow.data.protocols.db.UpdateUserProfileRepository;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
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
        UpdateUserProfileRepository {

  // Column name (Portuguese, matches usuarios.perfil) vs. bind parameter name (English, matches
  // this class's other :name/:phone/:createdAt placeholders) - two different strings, unlike
  // ServiceOrderJdbiRepository's STATUS constant where the column and bind name happen to match.
  private static final String PROFILE_COLUMN = "perfil";
  private static final String PROFILE_PARAM = "profile";

  private static final String SELECT_SQL =
      "SELECT id, nome, telefone, perfil, criado_em FROM usuarios";

  private static final String INSERT_SQL =
      """
      INSERT INTO usuarios (id, nome, telefone, perfil, criado_em)
      VALUES (:id, :name, :phone, :profile, :createdAt)
      """;

  private static final String UPDATE_PROFILE_SQL =
      "UPDATE usuarios SET perfil = :profile WHERE id = :id";

  private final Jdbi jdbi;

  public UserJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public User create(User user) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(INSERT_SQL)
                .bind("id", user.id())
                .bind("name", user.name())
                .bind("phone", user.phone())
                .bind(PROFILE_PARAM, user.profile().dbValue())
                .bind("createdAt", user.createdAt())
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

  @Override
  public User updateProfile(UUID id, UserProfile profile) {
    return jdbi.inTransaction(
        handle -> {
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
        rs.getTimestamp("criado_em").toInstant());
  }
}
