package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.CreateVehicleRepository;
import com.washflow.data.protocols.db.LoadVehicleByIdRepository;
import com.washflow.data.protocols.db.LoadVehicleByPlateRepository;
import com.washflow.data.protocols.db.LoadVehiclesByCustomerIdRepository;
import com.washflow.domain.entities.Vehicle;
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
        CreateVehicleRepository {

  private static final String SELECT_SQL =
      "SELECT id, cliente_id, placa, modelo, cor FROM veiculos";

  private static final String INSERT_SQL =
      """
      INSERT INTO veiculos (id, cliente_id, placa, modelo, cor)
      VALUES (:id, :customerId, :plate, :model, :color)
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
                .execute());

    return vehicle;
  }

  private Vehicle mapRow(ResultSet rs, StatementContext ctx) throws SQLException {
    return new Vehicle(
        (UUID) rs.getObject("id"),
        (UUID) rs.getObject("cliente_id"),
        rs.getString("placa"),
        rs.getString("modelo"),
        rs.getString("cor"));
  }
}
