package com.washflow.data.protocols.db;

import java.util.UUID;

public interface DeleteCashTransactionRepository {

  void deleteById(UUID transactionId);
}
