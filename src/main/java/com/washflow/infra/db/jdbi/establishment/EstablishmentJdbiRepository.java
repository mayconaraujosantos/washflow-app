package com.washflow.infra.db.jdbi.establishment;

import com.washflow.data.protocols.db.establishment.EstablishmentRepository;
import com.washflow.domain.entities.establishment.Establishment;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

/** The current request's lava-jato settings ({@code estabelecimentos} row of the tenant). */
public class EstablishmentJdbiRepository implements EstablishmentRepository {

  private static final String SELECT_SQL =
      """
      SELECT nome, hora_abertura, hora_fechamento, abre_domingo, max_veiculos_hora, criado_em
      FROM estabelecimentos WHERE id = :tenant
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
          handle
              .createUpdate(
                  """
                  UPDATE estabelecimentos
                  SET nome = :name, hora_abertura = :opening, hora_fechamento = :closing,
                      abre_domingo = :sunday, max_veiculos_hora = :capacity, atualizado_em = :now
                  WHERE id = :tenant
                  """)
              .bind("name", establishment.name())
              .bind("opening", establishment.openingHour())
              .bind("closing", establishment.closingHour())
              .bind("sunday", establishment.openOnSunday())
              .bind("capacity", establishment.maxVehiclesPerHour())
              .bind("now", Instant.now())
              .execute();
          return select(handle);
        });
  }

  private static Establishment select(Handle handle) {
    return handle.createQuery(SELECT_SQL).map((rs, ctx) -> map(rs)).one();
  }

  private static Establishment map(ResultSet rs) throws SQLException {
    return new Establishment(
        rs.getString("nome"),
        rs.getInt("hora_abertura"),
        rs.getInt("hora_fechamento"),
        rs.getBoolean("abre_domingo"),
        rs.getInt("max_veiculos_hora"),
        rs.getTimestamp("criado_em").toInstant());
  }
}
