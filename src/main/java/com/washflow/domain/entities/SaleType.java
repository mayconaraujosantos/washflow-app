package com.washflow.domain.entities;

/** {@code vendas.tipo}: a counter sale already delivered, or a quote for the customer. */
public enum SaleType {
  QUICK_SALE("VENDA"),
  BUDGET("ORCAMENTO");

  private final String dbValue;

  SaleType(String dbValue) {
    this.dbValue = dbValue;
  }

  public String dbValue() {
    return dbValue;
  }

  public static SaleType fromDbValue(String value) {
    for (SaleType type : values()) {
      if (type.dbValue.equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown sale type: " + value);
  }
}
