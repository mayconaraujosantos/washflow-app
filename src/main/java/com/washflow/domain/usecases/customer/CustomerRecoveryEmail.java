package com.washflow.domain.usecases.customer;

import java.util.Optional;
import java.util.UUID;

/** "Meus dados": the e-mail where the customer's password-reset codes go. */
public interface CustomerRecoveryEmail {

  Optional<String> load(UUID customerId);

  /** Saves it (blank clears it) and returns the e-mail now used for codes, if any. */
  Optional<String> update(UUID customerId, String email);
}
