package com.washflow.application.modules;

import com.washflow.data.usecases.pricing.DbCreateServicePrice;
import com.washflow.data.usecases.pricing.DbListServicePrices;
import com.washflow.domain.usecases.pricing.CreateServicePrice;
import com.washflow.domain.usecases.pricing.ListServicePrices;
import com.washflow.infra.db.jdbi.pricing.ServicePriceJdbiRepository;
import com.washflow.presentation.controllers.pricing.CreateServicePriceController;
import com.washflow.presentation.controllers.pricing.ListServicePricesController;
import com.washflow.presentation.protocols.Controller;
import org.jdbi.v3.core.Jdbi;

/** Composition root of the service catalog: builds its use cases and controllers. */
public final class PricingModule {

  private PricingModule() {}

  public static CreateServicePrice createServicePrice(Jdbi jdbi) {
    return new DbCreateServicePrice(new ServicePriceJdbiRepository(jdbi));
  }

  /**
   * Swapping JDBI for Hibernate later means changing only the {@code new
   * ServicePriceJdbiRepository(...)} call here - {@code DbListServicePrices} depends on the {@code
   * data.protocols.db} interfaces, not on this concrete class.
   */
  public static ListServicePrices listServicePrices(Jdbi jdbi) {
    return new DbListServicePrices(new ServicePriceJdbiRepository(jdbi));
  }

  public static Controller createServicePriceController(Jdbi jdbi) {
    return new CreateServicePriceController(createServicePrice(jdbi));
  }

  public static Controller listServicePricesController(Jdbi jdbi) {
    return new ListServicePricesController(listServicePrices(jdbi));
  }
}
