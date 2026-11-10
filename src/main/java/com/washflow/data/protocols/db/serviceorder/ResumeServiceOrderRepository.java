package com.washflow.data.protocols.db.serviceorder;

import java.util.UUID;

/** Port for resuming a paused wash. Returns true when the resume was successfully applied. */
public interface ResumeServiceOrderRepository {

  boolean resume(UUID orderId, UUID washerId);
}
