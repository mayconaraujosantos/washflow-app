package com.washflow.data.protocols.db;

import com.washflow.domain.entities.Sale;
import com.washflow.domain.entities.SaleRefund;
import com.washflow.domain.entities.SaleType;
import java.time.Instant;
import java.util.List;

public interface LoadSalesRepository {

  /** {@code start <= criado_em < end}, newest first; {@code type} null means both types. */
  List<Sale> loadBetween(Instant start, Instant end, SaleType type);

  /** Refunds made (by refund date) with {@code start <= criado_em < end}, newest first. */
  List<SaleRefund> loadRefundsBetween(Instant start, Instant end);
}
