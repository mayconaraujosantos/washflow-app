package com.washflow.data.protocols.db.finance;

import com.washflow.domain.entities.CashTransaction;

public interface UpdateCashTransactionRepository {

  CashTransaction update(CashTransaction transaction);
}
