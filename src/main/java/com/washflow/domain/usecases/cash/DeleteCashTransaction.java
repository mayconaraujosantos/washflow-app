package com.washflow.domain.usecases.cash;

import java.util.UUID;

public interface DeleteCashTransaction {

  void delete(UUID id);
}
