package com.washflow.data.protocols.db.finance;

import java.util.UUID;

public interface DeleteCashTransactionRepository {

  void deleteById(UUID transactionId);
}
