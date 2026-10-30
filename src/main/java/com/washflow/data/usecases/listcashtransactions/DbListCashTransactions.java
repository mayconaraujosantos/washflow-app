package com.washflow.data.usecases.listcashtransactions;

import com.washflow.data.protocols.db.LoadCashTransactionRepository;
import com.washflow.domain.entities.CashTransaction;
import com.washflow.domain.usecases.ListCashTransactions;
import java.time.LocalDate;
import java.util.List;

public class DbListCashTransactions implements ListCashTransactions {

  private final LoadCashTransactionRepository loadRepository;

  public DbListCashTransactions(LoadCashTransactionRepository loadRepository) {
    this.loadRepository = loadRepository;
  }

  @Override
  public List<CashTransaction> list(LocalDate from, LocalDate to) {
    return loadRepository.loadByDateRange(from, to);
  }

  @Override
  public List<CashTransaction> listAll() {
    return loadRepository.loadAll();
  }
}
