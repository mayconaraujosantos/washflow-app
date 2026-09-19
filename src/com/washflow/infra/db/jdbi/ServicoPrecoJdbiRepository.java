package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.BuscarServicoPrecoPorIdRepository;
import com.washflow.domain.entities.ServicoPreco;
import java.util.Optional;
import org.jdbi.v3.core.Jdbi;

public class ServicoPrecoJdbiRepository implements BuscarServicoPrecoPorIdRepository {

  private final Jdbi jdbi;

  public ServicoPrecoJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Optional<ServicoPreco> buscarPorId(int id) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT id, nome, preco, comissao_lavador FROM servicos_preco WHERE id = :id")
                .bind("id", id)
                .map(
                    (rs, ctx) ->
                        new ServicoPreco(
                            rs.getInt("id"),
                            rs.getString("nome"),
                            rs.getBigDecimal("preco"),
                            rs.getBigDecimal("comissao_lavador")))
                .findOne());
  }
}
