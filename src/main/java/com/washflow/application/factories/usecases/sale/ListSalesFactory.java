package com.washflow.application.factories.usecases.sale;

import com.washflow.data.usecases.sale.DbListSales;
import com.washflow.domain.usecases.sale.ListSales;
import com.washflow.infra.db.jdbi.SaleJdbiRepository;
import org.jdbi.v3.core.Jdbi;

public final class ListSalesFactory {

  private ListSalesFactory() {}

  public static ListSales make(Jdbi jdbi) {
    return new DbListSales(new SaleJdbiRepository(jdbi));
  }
}
