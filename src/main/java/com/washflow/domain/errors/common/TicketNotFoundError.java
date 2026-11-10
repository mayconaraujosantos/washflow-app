package com.washflow.domain.errors.common;

public class TicketNotFoundError extends Exception {

  public TicketNotFoundError() {
    super("Ticket não encontrado");
  }
}
