package com.washflow.domain.usecases.finance;

import com.washflow.domain.entities.finance.CashTransaction;
import java.util.UUID;

public interface UpdateCashTransaction {

  CashTransaction update(UUID id, CashTransaction.PartialUpdate update);
}
