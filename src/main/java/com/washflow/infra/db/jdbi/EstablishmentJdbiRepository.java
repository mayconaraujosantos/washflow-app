package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.EstablishmentRepository;
import com.washflow.domain.entities.Establishment;
import com.washflow.domain.entities.User;
import com.washflow.domain.errors.EstablishmentAlreadyConfiguredError;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

public class EstablishmentJdbiRepository implements EstablishmentRepository {

  private static final String SELECT_SQL =
      """
      SELECT nome, hora_abertura, hora_fechamento, abre_domingo, max_veiculos_hora, configurado_em
      FROM estabelecimento WHERE id = 1
      """;

  private final Jdbi jdbi;

  public EstablishmentJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Establishment load() {
    return jdbi.withHandle(EstablishmentJdbiRepository::select);
  }

  @Override
  public Establishment update(Establishment establishment) {
    return jdbi.inTransaction(
        handle -> {
          write(handle, establishment, false);
          return select(handle);
        });
  }

  @Override
  public User completeSetup(Establishment establishment, User owner, String passwordHash)
      throws EstablishmentAlreadyConfiguredError {
    boolean done =
        jdbi.inTransaction(
            handle -> {
              // Guard against two wizards racing: only the first one finds it unconfigured.
              if (write(handle, establishment, true) != 1) {
                return false;
              }
              handle
                  .createUpdate(
                      """
                      INSERT INTO usuarios
                        (id, nome, telefone, email, senha_hash, perfil, criado_em, ativo)
                      VALUES (:id, :name, :phone, :email, :hash, 'GERENTE', :createdAt, TRUE)
                      """)
                  .bind("id", owner.id())
                  .bind("name", owner.name())
                  .bind("phone", owner.phone())
                  .bind("email", owner.email())
                  .bind("hash", passwordHash)
                  .bind("createdAt", owner.createdAt())
                  .execute();
              return true;
            });
    if (!done) {
      throw new EstablishmentAlreadyConfiguredError();
    }
    return owner;
  }

  private static int write(Handle handle, Establishment establishment, boolean onlyIfUnconfigured) {
    return handle
        .createUpdate(
            """
            UPDATE estabelecimento
            SET nome = :name, hora_abertura = :opening, hora_fechamento = :closing,
                abre_domingo = :sunday, max_veiculos_hora = :capacity,
                configurado_em = :configuredAt, atualizado_em = :now
            WHERE id = 1
            """
                + (onlyIfUnconfigured ? " AND configurado_em IS NULL" : ""))
        .bind("name", establishment.name())
        .bind("opening", establishment.openingHour())
        .bind("closing", establishment.closingHour())
        .bind("sunday", establishment.openOnSunday())
        .bind("capacity", establishment.maxVehiclesPerHour())
        .bind("configuredAt", establishment.configuredAt())
        .bind("now", Instant.now())
        .execute();
  }

  private static Establishment select(Handle handle) {
    return handle.createQuery(SELECT_SQL).map((rs, ctx) -> map(rs)).one();
  }

  private static Establishment map(ResultSet rs) throws SQLException {
    Timestamp configuredAt = rs.getTimestamp("configurado_em");
    return new Establishment(
        rs.getString("nome"),
        rs.getInt("hora_abertura"),
        rs.getInt("hora_fechamento"),
        rs.getBoolean("abre_domingo"),
        rs.getInt("max_veiculos_hora"),
        configuredAt == null ? null : configuredAt.toInstant());
  }
}
