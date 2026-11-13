package com.washflow.data.protocols.db.serviceorder;

import java.util.UUID;

public interface AssignWasherRepository {

  /**
   * Sets (or clears, with {@code washerId} null) the washer of an order that hasn't started
   * washing; false when the order is no longer scheduled or waiting in the yard. The queue order
   * ({@code atualizado_em}) doesn't change.
   */
  boolean assignWasher(UUID serviceOrderId, UUID washerId);
}
