package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.listserviceprices.DbListServicePrices;
import com.washflow.domain.usecases.ListServicePrices;
import com.washflow.infra.db.jdbi.ServicePriceJdbiRepository;
import org.jdbi.v3.core.Jdbi;

/**
 * Swapping JDBI for Hibernate later means changing only the {@code new
 * ServicePriceJdbiRepository(...)} call here - {@code DbListServicePrices} depends on the {@code
 * data.protocols.db} interfaces, not on this concrete class.
 */
public final class ListServicePricesFactory {

  private ListServicePricesFactory() {}

  public static ListServicePrices make(Jdbi jdbi) {
    return new DbListServicePrices(new ServicePriceJdbiRepository(jdbi));
  }
}
