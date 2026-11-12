package com.washflow.domain.errors.user;

import com.washflow.domain.errors.common.ConflictError;

public class UserHasActiveWashesError extends ConflictError {

  public UserHasActiveWashesError() {
    super(
        "USER_HAS_ACTIVE_WASHES",
        "Conclua ou reatribua as lavagens em andamento deste lavador antes de alterar a conta");
  }
}
