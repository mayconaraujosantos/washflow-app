package com.washflow.data.usecases.customer;

import com.washflow.data.protocols.db.user.PasswordResetRepository;
import com.washflow.domain.entities.shared.ContactRules;
import com.washflow.domain.usecases.customer.CustomerRecoveryEmail;
import java.util.Optional;
import java.util.UUID;

public class DbCustomerRecoveryEmail implements CustomerRecoveryEmail {

  private final PasswordResetRepository passwordResetRepository;

  public DbCustomerRecoveryEmail(PasswordResetRepository passwordResetRepository) {
    this.passwordResetRepository = passwordResetRepository;
  }

  @Override
  public Optional<String> load(UUID customerId) {
    return passwordResetRepository.loadRecoveryEmail(customerId);
  }

  @Override
  public Optional<String> update(UUID customerId, String email) {
    passwordResetRepository.saveRecoveryEmail(customerId, ContactRules.normalizeEmail(email));
    return passwordResetRepository.loadRecoveryEmail(customerId);
  }
}
