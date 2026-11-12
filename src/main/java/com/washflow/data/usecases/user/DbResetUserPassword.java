package com.washflow.data.usecases.user;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.data.protocols.db.user.SaveUserPasswordHashRepository;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.user.PasswordResetNotAllowedError;
import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.usecases.user.ResetUserPassword;
import java.util.UUID;

public class DbResetUserPassword implements ResetUserPassword {

  static final int MIN_PASSWORD_LENGTH = 8;

  private final LoadUserByIdRepository loadUserByIdRepository;
  private final SaveUserPasswordHashRepository saveUserPasswordHashRepository;
  private final PasswordHasher passwordHasher;

  public DbResetUserPassword(
      LoadUserByIdRepository loadUserByIdRepository,
      SaveUserPasswordHashRepository saveUserPasswordHashRepository,
      PasswordHasher passwordHasher) {
    this.loadUserByIdRepository = loadUserByIdRepository;
    this.saveUserPasswordHashRepository = saveUserPasswordHashRepository;
    this.passwordHasher = passwordHasher;
  }

  @Override
  public User reset(UUID userId, String newPassword) {
    if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
      throw new IllegalArgumentException("A nova senha deve ter no mínimo 8 caracteres");
    }
    // Loads within the manager's lava-jato: an id from another one is "not found".
    User user =
        loadUserByIdRepository.loadById(userId).orElseThrow(() -> new UserNotFoundError(userId));
    if (user.profile() != UserProfile.CUSTOMER && user.profile() != UserProfile.WASHER) {
      throw new PasswordResetNotAllowedError();
    }
    saveUserPasswordHashRepository.savePasswordHash(userId, passwordHasher.hash(newPassword));
    return user;
  }
}
