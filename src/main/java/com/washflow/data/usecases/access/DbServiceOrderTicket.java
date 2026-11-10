package com.washflow.data.usecases.access;

import com.washflow.data.protocols.db.TicketRepository;
import com.washflow.domain.entities.TicketView;
import com.washflow.domain.errors.common.TicketNotFoundError;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.customer.ServiceOrderTicket;
import java.util.UUID;

public class DbServiceOrderTicket implements ServiceOrderTicket {

  private final TicketRepository ticketRepository;

  public DbServiceOrderTicket(TicketRepository ticketRepository) {
    this.ticketRepository = ticketRepository;
  }

  @Override
  public String issue(UUID serviceOrderId) throws ServiceOrderNotFoundError {
    return ticketRepository
        .issue(serviceOrderId, Secrets.token(24))
        .orElseThrow(() -> new ServiceOrderNotFoundError(serviceOrderId));
  }

  @Override
  public TicketView view(String token) throws TicketNotFoundError {
    if (token == null || !token.matches("[A-Za-z0-9_-]{16,64}")) {
      throw new TicketNotFoundError();
    }
    return ticketRepository.loadByToken(token).orElseThrow(TicketNotFoundError::new);
  }
}
