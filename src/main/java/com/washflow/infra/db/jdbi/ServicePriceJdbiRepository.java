package com.washflow.infra.db.jdbi;

import com.washflow.data.protocols.db.shared.CreateServicePriceRepository;
import com.washflow.data.protocols.db.shared.LoadAllServicePricesRepository;
import com.washflow.data.protocols.db.shared.LoadServicePriceByIdRepository;
import com.washflow.domain.entities.pricing.ServicePrice;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementContext;

public class ServicePriceJdbiRepository
    implements LoadServicePriceByIdRepository,
        LoadAllServicePricesRepository,
        CreateServicePriceRepository {

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
                .createQuery(SELECT_SQL + " WHERE id = :id AND estabelecimento_id = :tenant")
                .bind("id", id)
                .map(this::mapRow)
                .findOne());
  }

  @Override
  public List<ServicePrice> loadAll() {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(SELECT_SQL + " WHERE estabelecimento_id = :tenant ORDER BY nome")
                .map(this::mapRow)
                .list());
  }

  @Override
  public ServicePrice create(String name, BigDecimal price, BigDecimal washerCommission) {
    return jdbi.withHandle(
        handle -> {
          int id =
              handle
                  .createUpdate(
                      "INSERT INTO servicos_preco (estabelecimento_id, nome, preco, comissao_lavador)"
                          + " VALUES (:tenant, :name, :price, :commission)")
                  .bind("name", name)
                  .bind("price", price)
                  .bind("commission", washerCommission)
                  .executeAndReturnGeneratedKeys("id")
                  .mapTo(Integer.class)
                  .one();
          return new ServicePrice(id, name, price, washerCommission);
        });
  }

  private ServicePrice mapRow(ResultSet rs, StatementContext ctx) throws SQLException {
    return new ServicePrice(
        rs.getInt("id"),
        rs.getString("nome"),
        rs.getBigDecimal("preco"),
        rs.getBigDecimal("comissao_lavador"));
  }
}
