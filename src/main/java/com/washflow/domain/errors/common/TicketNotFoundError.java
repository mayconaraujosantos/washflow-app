package com.washflow.domain.errors.common;

public class TicketNotFoundError extends NotFoundError {

  public TicketNotFoundError() {
    super("TICKET_NOT_FOUND", "Ticket não encontrado");
  }
}
