package com.washflow.domain.errors.serviceorder;

import com.washflow.domain.errors.common.NotFoundError;

public class TicketNotFoundError extends NotFoundError {

  public TicketNotFoundError() {
    super("TICKET_NOT_FOUND", "Ticket não encontrado");
  }
}
