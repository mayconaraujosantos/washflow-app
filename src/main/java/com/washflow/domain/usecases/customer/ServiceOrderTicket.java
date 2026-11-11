package com.washflow.domain.usecases.customer;

import com.washflow.domain.entities.serviceorder.TicketView;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotFoundError;
import com.washflow.domain.errors.serviceorder.TicketNotFoundError;
import java.util.UUID;

/** Public, unguessable link (and QR) to follow one order without logging in. */
public interface ServiceOrderTicket {

  /** Returns the order's ticket token, creating it on first use. */
  String issue(UUID serviceOrderId) throws ServiceOrderNotFoundError;

  TicketView view(String token) throws TicketNotFoundError;
}
