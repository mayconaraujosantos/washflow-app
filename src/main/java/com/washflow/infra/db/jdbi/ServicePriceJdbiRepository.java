package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.LoadAllServicePricesRepository;
import com.washflow.data.protocols.db.LoadServicePriceByIdRepository;
import com.washflow.domain.entities.ServicePrice;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;

public class ServicePriceJdbiRepository
    implements LoadServicePriceByIdRepository, LoadAllServicePricesRepository {

  private static final String SELECT_SQL =
      "SELECT id, nome, preco, comissao_lavador FROM servicos_preco";

  private final Jdbi jdbi;

  public ServicePriceJdbiRepository(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public Optional<ServicePrice> loadById(int id) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_SQL + " WHERE id = :id")
                .bind("id", id)
                .map(this::mapRow)
                .findOne());
  }

  @Override
  public List<ServicePrice> loadAll() {
    return jdbi.withHandle(
        handle -> handle.createQuery(SELECT_SQL + " ORDER BY nome").map(this::mapRow).list());
  }

  private ServicePrice mapRow(ResultSet rs, StatementContext ctx) throws SQLException {
    return new ServicePrice(
        rs.getInt("id"),
        rs.getString("nome"),
        rs.getBigDecimal("preco"),
        rs.getBigDecimal("comissao_lavador"));
  }
}
