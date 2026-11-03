package com.washflow.data.usecases.listsales;

import com.washflow.data.protocols.db.LoadSalesRepository;
import com.washflow.data.usecases.scheduleserviceorder.SchedulingRules;
import com.washflow.domain.entities.Sale;
import com.washflow.domain.usecases.ListSales;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class DbListSales implements ListSales {

  private final LoadSalesRepository loadSalesRepository;

  public DbListSales(LoadSalesRepository loadSalesRepository) {
    this.loadSalesRepository = loadSalesRepository;
  }

  @Override
  public List<Sale> list(Params params) {
    if (params.to().isBefore(params.from())
        || ChronoUnit.DAYS.between(params.from(), params.to()) > 365) {
      throw new IllegalArgumentException("Range must be ordered and no longer than 366 days");
    }
    return loadSalesRepository.loadBetween(
        params.from().atStartOfDay(SchedulingRules.STORE_ZONE).toInstant(),
        params.to().plusDays(1).atStartOfDay(SchedulingRules.STORE_ZONE).toInstant(),
        params.type());
  }
}
