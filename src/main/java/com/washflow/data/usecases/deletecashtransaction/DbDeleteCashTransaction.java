package com.washflow.data.usecases.deletecashtransaction;

import com.washflow.data.protocols.db.DeleteCashTransactionRepository;
import com.washflow.domain.usecases.DeleteCashTransaction;
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
