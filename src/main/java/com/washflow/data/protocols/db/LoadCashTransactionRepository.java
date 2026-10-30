package com.washflow.data.protocols.db;

import com.washflow.domain.entities.CashTransaction;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LoadCashTransactionRepository {

  List<CashTransaction> loadByDateRange(LocalDate from, LocalDate to);

  List<CashTransaction> loadAll();

  CashTransaction loadById(UUID transactionId);
}
