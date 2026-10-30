package com.washflow.data.protocols.db;

import java.util.UUID;

/** Port for pausing an active wash. Returns true when the pause was successfully applied. */
public interface PauseServiceOrderRepository {

  boolean pause(UUID orderId, UUID washerId, String reason, String description);
}
