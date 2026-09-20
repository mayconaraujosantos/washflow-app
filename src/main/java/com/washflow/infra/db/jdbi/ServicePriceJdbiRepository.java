package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.LoadServicePriceByIdRepository;
import com.washflow.domain.entities.ServicePrice;
import java.util.Optional;
import org.jdbi.v3.core.Jdbi;

public class ServicePriceJdbiRepository implements LoadServicePriceByIdRepository {

  private final Jdbi jdbi;

  public ServicePriceJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Optional<ServicePrice> loadById(int id) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT id, nome, preco, comissao_lavador FROM servicos_preco WHERE id = :id")
                .bind("id", id)
                .map(
                    (rs, ctx) ->
                        new ServicePrice(
                            rs.getInt("id"),
                            rs.getString("nome"),
                            rs.getBigDecimal("preco"),
                            rs.getBigDecimal("comissao_lavador")))
                .findOne());
  }
}
