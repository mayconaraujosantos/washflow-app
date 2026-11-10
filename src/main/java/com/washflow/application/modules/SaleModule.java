package com.washflow.application.modules;

import com.washflow.data.usecases.sale.DbCreateSale;
import com.washflow.data.usecases.sale.DbListSales;
import com.washflow.data.usecases.sale.DbRefundSale;
import com.washflow.domain.usecases.sale.CreateSale;
import com.washflow.domain.usecases.sale.ListSales;
import com.washflow.domain.usecases.sale.RefundSale;
import com.washflow.infra.db.jdbi.pricing.ServicePriceJdbiRepository;
import com.washflow.infra.db.jdbi.sale.SaleJdbiRepository;
import com.washflow.infra.db.jdbi.vehicle.VehicleJdbiRepository;
import com.washflow.presentation.controllers.sale.CreateSaleController;
import com.washflow.presentation.controllers.sale.ListSaleRefundsController;
import com.washflow.presentation.controllers.sale.ListSalesController;
import com.washflow.presentation.controllers.sale.RefundSaleController;
import com.washflow.presentation.protocols.Controller;
import java.time.Clock;
import org.jdbi.v3.core.Jdbi;

/** Composition root of quick sales: builds its use cases and controllers. */
public final class SaleModule {

  private SaleModule() {}

  public static CreateSale createSale(Jdbi jdbi) {
    return new DbCreateSale(
        new ServicePriceJdbiRepository(jdbi),
        new VehicleJdbiRepository(jdbi),
        new SaleJdbiRepository(jdbi),
        Clock.systemUTC());
  }

  public static ListSales listSales(Jdbi jdbi) {
    return new DbListSales(new SaleJdbiRepository(jdbi));
  }

  public static RefundSale refundSale(Jdbi jdbi) {
    return new DbRefundSale(new SaleJdbiRepository(jdbi), Clock.systemUTC());
  }

  public static Controller createSaleController(Jdbi jdbi) {
    return new CreateSaleController(createSale(jdbi));
  }

  public static Controller listSaleRefundsController(Jdbi jdbi) {
    return new ListSaleRefundsController(listSales(jdbi));
  }

  public static Controller listSalesController(Jdbi jdbi) {
    return new ListSalesController(listSales(jdbi));
  }

  public static Controller refundSaleController(Jdbi jdbi) {
    return new RefundSaleController(refundSale(jdbi));
  }
}
