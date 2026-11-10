package com.washflow.data.usecases.user;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.TokenHasher;
import com.washflow.data.protocols.db.user.LoadUserByEmailRepository;
import com.washflow.data.protocols.db.user.LoadUserByPhoneRepository;
import com.washflow.data.protocols.db.user.PasswordResetRepository;
import com.washflow.data.protocols.db.user.SaveUserPasswordHashRepository;
import com.washflow.data.protocols.messaging.EmailSender;
import com.washflow.data.usecases.shared.Secrets;
import com.washflow.domain.entities.shared.ContactRules;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.auth.InvalidOtpError;
import com.washflow.domain.errors.user.EmailDeliveryFailedError;
import com.washflow.domain.errors.user.EmailResetUnavailableError;
import com.washflow.domain.usecases.user.ResetPasswordByEmail;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The code is stored only as a keyed hash, expires after {@link #CODE_TTL}, allows {@link
 * #MAX_ATTEMPTS} guesses and can be re-sent after {@link #RESEND_AFTER}.
 */
@SuppressWarnings("java:S107") // one port per collaborator, like the other Db* use cases
public class DbResetPasswordByEmail implements ResetPasswordByEmail {

  static final Duration CODE_TTL = Duration.ofMinutes(10);
  static final Duration RESEND_AFTER = Duration.ofSeconds(60);
  static final int MAX_ATTEMPTS = 5;
  static final int MIN_PASSWORD_LENGTH = 8;
  private static final Logger LOGGER = LoggerFactory.getLogger(DbResetPasswordByEmail.class);

  private final LoadUserByPhoneRepository loadUserByPhoneRepository;
  private final LoadUserByEmailRepository loadUserByEmailRepository;
  private final PasswordResetRepository passwordResetRepository;
  private final SaveUserPasswordHashRepository saveUserPasswordHashRepository;
  private final PasswordHasher passwordHasher;
  private final TokenHasher tokenHasher;
  private final EmailSender emailSender;
  private final Clock clock;

  public DbResetPasswordByEmail(
      LoadUserByPhoneRepository loadUserByPhoneRepository,
      LoadUserByEmailRepository loadUserByEmailRepository,
      PasswordResetRepository passwordResetRepository,
      SaveUserPasswordHashRepository saveUserPasswordHashRepository,
      PasswordHasher passwordHasher,
      TokenHasher tokenHasher,
      EmailSender emailSender,
      Clock clock) {
    this.loadUserByPhoneRepository = loadUserByPhoneRepository;
    this.loadUserByEmailRepository = loadUserByEmailRepository;
    this.passwordResetRepository = passwordResetRepository;
    this.saveUserPasswordHashRepository = saveUserPasswordHashRepository;
    this.passwordHasher = passwordHasher;
    this.tokenHasher = tokenHasher;
    this.emailSender = emailSender;
    this.clock = clock;
  }

  @Override
  public boolean available() {
    return emailSender.available();
  }

  @Override
  public int requestForCustomer(String phone)
      throws EmailResetUnavailableError, EmailDeliveryFailedError {
    Optional<User> customer = customer(requiredPhone(phone));
    return request(
        customer, customer.flatMap(found -> passwordResetRepository.loadRecoveryEmail(found.id())));
  }

  @Override
  public int requestForManager(String email)
      throws EmailResetUnavailableError, EmailDeliveryFailedError {
    Optional<User> manager = manager(requiredEmail(email));
    return request(manager, manager.map(User::email));
  }

  @Override
  public void confirmForCustomer(String phone, String code, String newPassword)
      throws InvalidOtpError {
    requirePasswordLength(newPassword);
    String normalized = ContactRules.normalizePhone(phone);
    confirm(normalized == null ? Optional.empty() : customer(normalized), code, newPassword);
  }

  @Override
  public void confirmForManager(String email, String code, String newPassword)
      throws InvalidOtpError {
    requirePasswordLength(newPassword);
    String normalized = ContactRules.normalizeEmail(email);
    confirm(normalized == null ? Optional.empty() : manager(normalized), code, newPassword);
  }

  private int request(Optional<User> account, Optional<String> recipient)
      throws EmailResetUnavailableError, EmailDeliveryFailedError {
    if (!emailSender.available()) {
      throw new EmailResetUnavailableError();
    }
    int ttl = (int) CODE_TTL.toSeconds();
    // Same answer when there is no account, no e-mail or a code was just sent.
    if (account.isEmpty() || recipient.isEmpty()) {
      return ttl;
    }
    User user = account.get();
    Instant now = Instant.now(clock);
    Optional<PasswordResetRepository.PendingCode> pending =
        passwordResetRepository.loadCode(user.id());
    if (pending.isPresent() && pending.get().createdAt().plus(RESEND_AFTER).isAfter(now)) {
      return ttl;
    }
    String code = Secrets.digits(6);
    passwordResetRepository.saveCode(user.id(), hash(user, code), now.plus(CODE_TTL), now);
    try {
      emailSender.send(recipient.get(), "Seu código para criar uma nova senha", body(user, code));
    } catch (EmailSender.EmailDeliveryException e) {
      LOGGER.error("Password reset e-mail not sent for user {}", user.id(), e);
      passwordResetRepository.deleteCode(user.id());
      throw new EmailDeliveryFailedError();
    }
    return ttl;
  }

  private void confirm(Optional<User> account, String code, String newPassword)
      throws InvalidOtpError {
    User user = account.orElseThrow(InvalidOtpError::new);
    PasswordResetRepository.PendingCode pending =
        passwordResetRepository.loadCode(user.id()).orElseThrow(InvalidOtpError::new);
    if (pending.expiresAt().isBefore(Instant.now(clock)) || pending.attempts() >= MAX_ATTEMPTS) {
      passwordResetRepository.deleteCode(user.id());
      throw new InvalidOtpError();
    }
    if (code == null
        || !MessageDigest.isEqual(
            pending.codeHash().getBytes(StandardCharsets.UTF_8),
            hash(user, code.trim()).getBytes(StandardCharsets.UTF_8))) {
      passwordResetRepository.incrementAttempts(user.id());
      throw new InvalidOtpError();
    }
    saveUserPasswordHashRepository.savePasswordHash(user.id(), passwordHasher.hash(newPassword));
    passwordResetRepository.deleteCode(user.id());
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

  private String hash(User user, String code) {
    return tokenHasher.hash("password-reset:" + user.id() + ":" + code);
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
        + CODE_TTL.toMinutes()
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
