package com.washflow.domain.errors.user;

import com.washflow.domain.errors.common.TooManyRequestsError;

public class AccountLockedError extends TooManyRequestsError {

  public AccountLockedError() {
    super("ACCOUNT_LOCKED", "Muitas tentativas. Tente novamente em alguns minutos");
  }
}
