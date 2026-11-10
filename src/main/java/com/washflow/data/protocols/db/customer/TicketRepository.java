package com.washflow.data.protocols.db.customer;

import com.washflow.domain.entities.serviceorder.TicketView;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository {

  /** Stores {@code candidate} unless the order already has a token; returns the token in use. */
  Optional<String> issue(UUID serviceOrderId, String candidate);

  Optional<TicketView> loadByToken(String token);
}
