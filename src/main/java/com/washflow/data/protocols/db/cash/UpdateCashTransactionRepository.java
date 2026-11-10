package com.washflow.data.protocols.db.cash;

import com.washflow.domain.entities.CashTransaction;

public interface UpdateCashTransactionRepository {

  CashTransaction update(CashTransaction transaction);
}
