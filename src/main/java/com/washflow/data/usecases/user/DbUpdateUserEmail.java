package com.washflow.data.usecases.user;

import com.washflow.data.protocols.db.user.LoadUserByEmailRepository;
import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.data.protocols.db.user.UpdateUserEmailRepository;
import com.washflow.data.usecases.shared.ContactRules;
import com.washflow.domain.entities.User;
import com.washflow.domain.errors.user.EmailAlreadyRegisteredError;
import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.usecases.user.UpdateUserEmail;
import java.util.UUID;

public class DbUpdateUserEmail implements UpdateUserEmail {

  private final LoadUserByIdRepository loadUserByIdRepository;
  private final LoadUserByEmailRepository loadUserByEmailRepository;
  private final UpdateUserEmailRepository updateUserEmailRepository;

  public DbUpdateUserEmail(
      LoadUserByIdRepository loadUserByIdRepository,
      LoadUserByEmailRepository loadUserByEmailRepository,
      UpdateUserEmailRepository updateUserEmailRepository) {
    this.loadUserByIdRepository = loadUserByIdRepository;
    this.loadUserByEmailRepository = loadUserByEmailRepository;
    this.updateUserEmailRepository = updateUserEmailRepository;
  }

  @Override
  public User update(UUID userId, String rawEmail)
      throws UserNotFoundError, EmailAlreadyRegisteredError {
    loadUserByIdRepository.loadById(userId).orElseThrow(() -> new UserNotFoundError(userId));
    String email = ContactRules.normalizeEmail(rawEmail);
    if (email != null) {
      var owner = loadUserByEmailRepository.loadByEmail(email);
      if (owner.isPresent() && !owner.get().id().equals(userId)) {
        throw new EmailAlreadyRegisteredError();
      }
    }
    return updateUserEmailRepository.updateEmail(userId, email);
  }
}
