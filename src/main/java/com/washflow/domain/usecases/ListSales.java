package com.washflow.domain.usecases;

import com.washflow.domain.entities.Sale;
import com.washflow.domain.entities.SaleRefund;
import com.washflow.domain.entities.SaleType;
import java.time.LocalDate;
import java.util.List;

/** Sales/budgets created between two shop-local dates (max 366 days); {@code type} optional. */
public interface ListSales {

  List<Sale> list(Params params);

  /** Sale refunds made between two shop-local dates (by refund date, max 366 days). */
  List<SaleRefund> refunds(LocalDate from, LocalDate to);

  record Params(SaleType type, LocalDate from, LocalDate to) {}
}
