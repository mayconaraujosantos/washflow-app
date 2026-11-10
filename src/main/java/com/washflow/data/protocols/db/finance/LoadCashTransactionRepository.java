package com.washflow.data.protocols.db.finance;

import com.washflow.domain.entities.finance.CashTransaction;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LoadCashTransactionRepository {

  List<CashTransaction> loadByDateRange(LocalDate from, LocalDate to);

  List<CashTransaction> loadAll();

  CashTransaction loadById(UUID transactionId);
}
