package com.washflow.data.protocols.db;

import com.washflow.domain.entities.CashTransaction;

public interface SaveCashTransactionRepository {

  CashTransaction save(CashTransaction transaction);
}
