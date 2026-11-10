package com.washflow.data.protocols.db.serviceorder;

import java.util.UUID;

/** Port for cancelling an order before washing. Returns true when the order was cancelled. */
public interface CancelServiceOrderRepository {

  boolean cancel(UUID orderId, UUID managerId, String reason, String description);
}
