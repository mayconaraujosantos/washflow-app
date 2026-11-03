package com.washflow.data.protocols.db;

import com.washflow.domain.entities.Sale;

public interface SaveSaleRepository {

  /** Persists the sale, its items and payments atomically. */
  Sale save(Sale sale);
}
