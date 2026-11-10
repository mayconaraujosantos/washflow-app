package com.washflow.domain.usecases.finance;

import java.util.UUID;

public interface DeleteCashTransaction {

  void delete(UUID id);
}
