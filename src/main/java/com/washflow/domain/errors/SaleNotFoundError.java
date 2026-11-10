package com.washflow.domain.errors;

public class SaleNotFoundError extends Exception {

  public SaleNotFoundError() {
    super("Venda não encontrada");
  }
}
