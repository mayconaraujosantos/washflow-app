package com.washflow.data.usecases.finance;

import com.washflow.data.protocols.db.finance.DeleteCashTransactionRepository;
import com.washflow.domain.usecases.finance.DeleteCashTransaction;
import java.util.UUID;

public class DbDeleteCashTransaction implements DeleteCashTransaction {

  private final DeleteCashTransactionRepository deleteRepository;

  public DbDeleteCashTransaction(DeleteCashTransactionRepository deleteRepository) {
    this.deleteRepository = deleteRepository;
  }

  @Override
  public void delete(UUID id) {
    deleteRepository.deleteById(id);
  }
}
