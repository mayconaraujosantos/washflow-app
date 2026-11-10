package com.washflow.application.factories.usecases;

import com.washflow.data.usecases.refundsale.DbRefundSale;
import com.washflow.domain.usecases.RefundSale;
import com.washflow.infra.db.jdbi.SaleJdbiRepository;
import java.time.Clock;
import org.jdbi.v3.core.Jdbi;

public final class RefundSaleFactory {

  private RefundSaleFactory() {}

  public static RefundSale make(Jdbi jdbi) {
    return new DbRefundSale(new SaleJdbiRepository(jdbi), Clock.systemUTC());
  }
}
