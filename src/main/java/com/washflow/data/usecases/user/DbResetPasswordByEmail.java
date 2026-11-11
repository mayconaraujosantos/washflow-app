package com.washflow.data.usecases.user;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.db.user.LoadUserByEmailRepository;
import com.washflow.data.protocols.db.user.LoadUserByPhoneRepository;
import com.washflow.data.protocols.db.user.PasswordResetRepository;
import com.washflow.data.protocols.db.user.SaveUserPasswordHashRepository;
import com.washflow.data.protocols.messaging.EmailDeliveryException;
import com.washflow.data.protocols.messaging.EmailSender;
import com.washflow.domain.entities.shared.ContactRules;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.auth.InvalidOtpError;
import com.washflow.domain.errors.user.EmailDeliveryFailedError;
import com.washflow.domain.errors.user.EmailResetUnavailableError;
import com.washflow.domain.usecases.user.ResetPasswordByEmail;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * "Esqueci a senha" by e-mail for customers (by phone, sent to the recovery e-mail) and managers
 * (by access e-mail). The request answers the same whether the account exists or not; the code
 * itself is handled by {@link PasswordResetCodes}.
 */
public class DbResetPasswordByEmail implements ResetPasswordByEmail {

  static final int MIN_PASSWORD_LENGTH = 8;
  private static final Logger LOGGER = LoggerFactory.getLogger(DbResetPasswordByEmail.class);

  private final LoadUserByPhoneRepository loadUserByPhoneRepository;
  private final LoadUserByEmailRepository loadUserByEmailRepository;
  private final PasswordResetRepository passwordResetRepository;
  private final SaveUserPasswordHashRepository saveUserPasswordHashRepository;
  private final PasswordHasher passwordHasher;
  private final EmailSender emailSender;
  private final PasswordResetCodes codes;

  public DbResetPasswordByEmail(
      LoadUserByPhoneRepository loadUserByPhoneRepository,
      LoadUserByEmailRepository loadUserByEmailRepository,
      PasswordResetRepository passwordResetRepository,
      SaveUserPasswordHashRepository saveUserPasswordHashRepository,
      PasswordHasher passwordHasher,
      EmailSender emailSender,
      PasswordResetCodes codes) {
    this.loadUserByPhoneRepository = loadUserByPhoneRepository;
    this.loadUserByEmailRepository = loadUserByEmailRepository;
    this.passwordResetRepository = passwordResetRepository;
    this.saveUserPasswordHashRepository = saveUserPasswordHashRepository;
    this.passwordHasher = passwordHasher;
    this.emailSender = emailSender;
    this.codes = codes;
  }

  @Override
  public boolean available() {
    return emailSender.available();
  }

  @Override
  public int requestForCustomer(String phone) {
    Optional<User> customer = customer(requiredPhone(phone));
    return request(
        customer, customer.flatMap(found -> passwordResetRepository.loadRecoveryEmail(found.id())));
  }

  @Override
  public int requestForManager(String email) {
    Optional<User> manager = manager(requiredEmail(email));
    return request(manager, manager.map(User::email));
  }

  @Override
  public void confirmForCustomer(String phone, String code, String newPassword) {
    requirePasswordLength(newPassword);
    String normalized = ContactRules.normalizePhone(phone);
    confirm(normalized == null ? Optional.empty() : customer(normalized), code, newPassword);
  }

  @Override
  public void confirmForManager(String email, String code, String newPassword) {
    requirePasswordLength(newPassword);
    String normalized = ContactRules.normalizeEmail(email);
    confirm(normalized == null ? Optional.empty() : manager(normalized), code, newPassword);
  }

  private int request(Optional<User> account, Optional<String> recipient) {
    if (!emailSender.available()) {
      throw new EmailResetUnavailableError();
    }
    // Same answer when there is no account, no e-mail or a code was just sent.
    if (account.isPresent() && recipient.isPresent()) {
      User user = account.get();
      Optional<String> code = codes.issue(user.id());
      if (code.isPresent()) {
        send(user, recipient.get(), code.get());
      }
    }
    return codes.ttlSeconds();
  }

  private void send(User user, String recipient, String code) {
    try {
      emailSender.send(recipient, "Seu código para criar uma nova senha", body(user, code));
    } catch (EmailDeliveryException e) {
      LOGGER.error("Password reset e-mail not sent for user {}", user.id(), e);
      codes.discard(user.id());
      throw new EmailDeliveryFailedError();
    }
  }

  private void confirm(Optional<User> account, String code, String newPassword) {
    User user = account.orElseThrow(InvalidOtpError::new);
    codes.verify(user.id(), code);
    saveUserPasswordHashRepository.savePasswordHash(user.id(), passwordHasher.hash(newPassword));
    codes.discard(user.id());
  }

  private Optional<User> customer(String phone) {
    return loadUserByPhoneRepository
        .loadByPhone(phone)
        .filter(user -> user.profile() == UserProfile.CUSTOMER && user.active());
  }

  private Optional<User> manager(String email) {
    return loadUserByEmailRepository
        .loadByEmail(email)
        .filter(user -> user.profile() == UserProfile.MANAGER && user.active());
  }

  private static String body(User user, String code) {
    String name = user.name() == null ? "" : user.name().trim().split("\\s+")[0];
    return "Olá"
        + (name.isEmpty() ? "" : ", " + name)
        + "!\n\n"
        + "Seu código para criar uma nova senha no Washflow é:\n\n"
        + "    "
        + code
        + "\n\n"
        + "Ele vale por "
        + PasswordResetCodes.CODE_TTL.toMinutes()
        + " minutos. Se não foi você que pediu, ignore este e-mail: sua senha continua a mesma.\n";
  }

  private static String requiredPhone(String raw) {
    String phone = ContactRules.normalizePhone(raw);
    if (phone == null) {
      throw new IllegalArgumentException("Informe o telefone com DDD");
    }
    return phone;
  }

  private static String requiredEmail(String raw) {
    String email = ContactRules.normalizeEmail(raw);
    if (email == null) {
      throw new IllegalArgumentException("Informe o e-mail");
    }
    return email;
  }

  private static void requirePasswordLength(String password) {
    if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
      throw new IllegalArgumentException("A nova senha deve ter no mínimo 8 caracteres");
    }
  }
}
