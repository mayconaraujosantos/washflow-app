package com.washflow.domain.usecases;

import com.washflow.domain.entities.CashTransaction;
import java.util.UUID;

public interface UpdateCashTransaction {

  CashTransaction update(UUID id, CashTransaction.PartialUpdate update);
}
