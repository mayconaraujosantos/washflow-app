package com.washflow.domain.errors;

public class TicketNotFoundError extends Exception {

  public TicketNotFoundError() {
    super("Ticket não encontrado");
  }
}
