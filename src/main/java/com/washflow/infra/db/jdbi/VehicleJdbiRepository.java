package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.CreateVehicleRepository;
import com.washflow.data.protocols.db.LoadVehicleByIdRepository;
import com.washflow.data.protocols.db.LoadVehicleByPlateRepository;
import com.washflow.data.protocols.db.LoadVehiclesByCustomerIdRepository;
import com.washflow.data.protocols.db.SearchVehiclesRepository;
import com.washflow.domain.entities.Vehicle;
import com.washflow.domain.entities.VehicleLookup;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;

public class VehicleJdbiRepository
    implements LoadVehicleByIdRepository,
        LoadVehicleByPlateRepository,
        LoadVehiclesByCustomerIdRepository,
        CreateVehicleRepository,
        SearchVehiclesRepository {

  private static final String SELECT_SQL =
      "SELECT id, cliente_id, placa, modelo, cor, contato_nome, contato_telefone FROM veiculos";

  private static final String INSERT_SQL =
      """
      INSERT INTO veiculos (id, cliente_id, placa, modelo, cor, contato_nome, contato_telefone)
      VALUES (:id, :customerId, :plate, :model, :color, :contactName, :contactPhone)
      """;

  // Registered owner first, walk-in contact as fallback - see Vehicle.contactName. Patterns are
  // built from normalized input (DbSearchVehicles): plate is [A-Z0-9], digits are [0-9], and the
  // free-text name has LIKE wildcards escaped below.
  private static final String SEARCH_SQL =
      """
      SELECT v.id, v.placa, v.modelo, v.cor, v.cliente_id,
             COALESCE(c.nome, v.contato_nome) AS owner_name,
             COALESCE(c.telefone, v.contato_telefone) AS owner_phone
      FROM veiculos v
      LEFT JOIN usuarios c ON c.id = v.cliente_id
      WHERE (:platePrefix <> '' AND v.placa LIKE :platePattern)
         OR (:text <> ''
             AND LOWER(COALESCE(c.nome, v.contato_nome, '')) LIKE :textPattern ESCAPE '!')
         OR (:digits <> ''
             AND COALESCE(c.telefone, v.contato_telefone, '') LIKE :digitsPattern)
      ORDER BY v.placa
      LIMIT :limit
      """;

  private final Jdbi jdbi;

  public VehicleJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Optional<Vehicle> loadById(UUID id) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_SQL + " WHERE id = :id")
                .bind("id", id)
                .map(this::mapRow)
                .findOne());
  }

  @Override
  public Optional<Vehicle> loadByPlate(String plate) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_SQL + " WHERE placa = :plate")
                .bind("plate", plate)
                .map(this::mapRow)
                .findOne());
  }

  @Override
  public List<Vehicle> loadByCustomerId(UUID customerId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_SQL + " WHERE cliente_id = :customerId")
                .bind("customerId", customerId)
                .map(this::mapRow)
                .list());
  }

  @Override
  public Vehicle create(Vehicle vehicle) {
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(INSERT_SQL)
                .bind("id", vehicle.id())
                .bind("customerId", vehicle.customerId())
                .bind("plate", vehicle.plate())
                .bind("model", vehicle.model())
                .bind("color", vehicle.color())
                .bind("contactName", vehicle.contactName())
                .bind("contactPhone", vehicle.contactPhone())
                .execute());

    return vehicle;
  }

  private Vehicle mapRow(ResultSet rs, StatementContext ctx) throws SQLException {
    return new Vehicle(
        (UUID) rs.getObject("id"),
        (UUID) rs.getObject("cliente_id"),
        rs.getString("placa"),
        rs.getString("modelo"),
        rs.getString("cor"),
        rs.getString("contato_nome"),
        rs.getString("contato_telefone"));
  }

  @Override
  public List<VehicleLookup> search(String platePrefix, String text, String digits, int limit) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SEARCH_SQL)
                .bind("platePrefix", platePrefix)
                .bind("platePattern", platePrefix + "%")
                .bind("text", text)
                .bind("textPattern", "%" + escapeLike(text) + "%")
                .bind("digits", digits)
                .bind("digitsPattern", "%" + digits + "%")
                .bind("limit", limit)
                .map(
                    (rs, ctx) ->
                        new VehicleLookup(
                            (UUID) rs.getObject("id"),
                            rs.getString("placa"),
                            rs.getString("modelo"),
                            rs.getString("cor"),
                            (UUID) rs.getObject("cliente_id"),
                            rs.getString("owner_name"),
                            rs.getString("owner_phone")))
                .list());
  }

  // The name is free text, so % and _ must match literally ('!' is the ESCAPE char above).
  private static String escapeLike(String value) {
    return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
  }
}
