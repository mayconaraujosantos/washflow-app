package com.washflow.domain.errors.establishment;

import com.washflow.domain.errors.common.AccessDeniedError;

public class InvalidSetupCodeError extends AccessDeniedError {

  public InvalidSetupCodeError() {
    super("INVALID_SETUP_CODE", "Código de configuração inválido");
  }
}
