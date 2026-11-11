package com.washflow.domain.errors.sale;

import com.washflow.domain.errors.common.NotFoundError;

public class SaleNotFoundError extends NotFoundError {

  public SaleNotFoundError() {
    super("SALE_NOT_FOUND", "Venda não encontrada");
  }
}
