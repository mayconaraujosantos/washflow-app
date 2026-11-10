package com.washflow.application.factories.usecases.sale;

import com.washflow.data.usecases.sale.DbRefundSale;
import com.washflow.domain.usecases.sale.RefundSale;
import com.washflow.infra.db.jdbi.sale.SaleJdbiRepository;
import java.time.Clock;
import org.jdbi.v3.core.Jdbi;

public final class RefundSaleFactory {

  private RefundSaleFactory() {}

  public static RefundSale make(Jdbi jdbi) {
    return new DbRefundSale(new SaleJdbiRepository(jdbi), Clock.systemUTC());
  }
}
