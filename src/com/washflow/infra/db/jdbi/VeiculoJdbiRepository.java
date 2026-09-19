package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.BuscarVeiculoPorIdRepository;
import com.washflow.domain.entities.Veiculo;
import java.util.Optional;
import java.util.UUID;
import org.jdbi.v3.core.Jdbi;

public class VeiculoJdbiRepository implements BuscarVeiculoPorIdRepository {

  private final Jdbi jdbi;

  public VeiculoJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Optional<Veiculo> buscarPorId(UUID id) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT id, cliente_id, placa, modelo, cor FROM veiculos WHERE id = :id")
                .bind("id", id)
                .map(
                    (rs, ctx) ->
                        new Veiculo(
                            (UUID) rs.getObject("id"),
                            (UUID) rs.getObject("cliente_id"),
                            rs.getString("placa"),
                            rs.getString("modelo"),
                            rs.getString("cor")))
                .findOne());
  }
}
