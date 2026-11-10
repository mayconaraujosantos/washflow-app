package com.washflow.data.protocols.db.sale;

import com.washflow.domain.entities.sale.Sale;
import com.washflow.domain.entities.sale.SaleRefund;
import com.washflow.domain.entities.sale.SaleType;
import java.time.Instant;
import java.util.List;

public interface LoadSalesRepository {

  /** {@code start <= criado_em < end}, newest first; {@code type} null means both types. */
  List<Sale> loadBetween(Instant start, Instant end, SaleType type);

  /** Refunds made (by refund date) with {@code start <= criado_em < end}, newest first. */
  List<SaleRefund> loadRefundsBetween(Instant start, Instant end);
}
