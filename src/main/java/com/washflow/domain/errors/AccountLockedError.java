package com.washflow.domain.errors;

public class AccountLockedError extends Exception {

  public AccountLockedError() {
    super("Muitas tentativas. Tente novamente em alguns minutos");
  }
}
