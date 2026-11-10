package com.washflow.data.protocols.db.cash;

import java.util.UUID;

public interface DeleteCashTransactionRepository {

  void deleteById(UUID transactionId);
}
