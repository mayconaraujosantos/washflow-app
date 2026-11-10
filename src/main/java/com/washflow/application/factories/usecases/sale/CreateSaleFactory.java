package com.washflow.application.factories.usecases.sale;

import com.washflow.data.usecases.sale.DbCreateSale;
import com.washflow.domain.usecases.sale.CreateSale;
import com.washflow.infra.db.jdbi.SaleJdbiRepository;
import com.washflow.infra.db.jdbi.ServicePriceJdbiRepository;
import com.washflow.infra.db.jdbi.VehicleJdbiRepository;
import java.time.Clock;
import org.jdbi.v3.core.Jdbi;

public final class CreateSaleFactory {

  private CreateSaleFactory() {}

  public static CreateSale make(Jdbi jdbi) {
    return new DbCreateSale(
        new ServicePriceJdbiRepository(jdbi),
        new VehicleJdbiRepository(jdbi),
        new SaleJdbiRepository(jdbi),
        Clock.systemUTC());
  }
}
