package com.washflow.data.usecases.sale;

import com.washflow.data.protocols.db.sale.LoadSalesRepository;
import com.washflow.data.usecases.shared.SchedulingRules;
import com.washflow.domain.entities.sale.Sale;
import com.washflow.domain.entities.sale.SaleRefund;
import com.washflow.domain.usecases.sale.ListSales;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class DbListSales implements ListSales {

  private final LoadSalesRepository loadSalesRepository;

  public DbListSales(LoadSalesRepository loadSalesRepository) {
    this.loadSalesRepository = loadSalesRepository;
  }

  @Override
  public List<Sale> list(Params params) {
    checkRange(params.from(), params.to());
    return loadSalesRepository.loadBetween(
        params.from().atStartOfDay(SchedulingRules.STORE_ZONE).toInstant(),
        params.to().plusDays(1).atStartOfDay(SchedulingRules.STORE_ZONE).toInstant(),
        params.type());
  }

  @Override
  public List<SaleRefund> refunds(LocalDate from, LocalDate to) {
    checkRange(from, to);
    return loadSalesRepository.loadRefundsBetween(
        from.atStartOfDay(SchedulingRules.STORE_ZONE).toInstant(),
        to.plusDays(1).atStartOfDay(SchedulingRules.STORE_ZONE).toInstant());
  }

  private static void checkRange(LocalDate from, LocalDate to) {
    if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 365) {
      throw new IllegalArgumentException("Range must be ordered and no longer than 366 days");
    }
  }
}
