package com.washflow.data.usecases.access;

import com.washflow.data.protocols.TokenHasher;
import com.washflow.data.protocols.db.CreateUserRepository;
import com.washflow.data.protocols.db.CustomerIdentityRepository;
import com.washflow.data.protocols.db.LoadUserByEmailRepository;
import com.washflow.data.protocols.db.LoadUserByPhoneRepository;
import com.washflow.data.protocols.db.OtpRepository;
import com.washflow.data.protocols.identity.GoogleTokenVerifier;
import com.washflow.data.protocols.messaging.OtpSender;
import com.washflow.data.usecases.shared.ContactRules;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.GoogleLoginUnavailableError;
import com.washflow.domain.errors.InvalidCredentialsError;
import com.washflow.domain.errors.InvalidOtpError;
import com.washflow.domain.errors.OtpTooSoonError;
import com.washflow.domain.errors.OtpUnavailableError;
import com.washflow.domain.errors.UserProfileMismatchError;
import com.washflow.domain.usecases.CustomerPasswordlessLogin;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * The code is stored only as a keyed hash, expires after {@link #CODE_TTL}, allows {@link
 * #MAX_ATTEMPTS} guesses and can be re-sent after {@link #RESEND_AFTER}. The request answer is the
 * same whether the phone has an account or not.
 */
@SuppressWarnings("java:S107") // one port per collaborator, like the other Db* use cases
public class DbCustomerPasswordlessLogin implements CustomerPasswordlessLogin {

  static final Duration CODE_TTL = Duration.ofMinutes(10);
  static final Duration RESEND_AFTER = Duration.ofSeconds(60);
  static final int MAX_ATTEMPTS = 5;

  private final OtpRepository otpRepository;
  private final OtpSender otpSender;
  private final TokenHasher tokenHasher;
  private final LoadUserByPhoneRepository loadUserByPhoneRepository;
  private final LoadUserByEmailRepository loadUserByEmailRepository;
  private final CreateUserRepository createUserRepository;
  private final CustomerIdentityRepository customerIdentityRepository;
  private final GoogleTokenVerifier googleTokenVerifier;
  private final String publicAppUrl;
  private final Clock clock;

  public DbCustomerPasswordlessLogin(
      OtpRepository otpRepository,
      OtpSender otpSender,
      TokenHasher tokenHasher,
      LoadUserByPhoneRepository loadUserByPhoneRepository,
      LoadUserByEmailRepository loadUserByEmailRepository,
      CreateUserRepository createUserRepository,
      CustomerIdentityRepository customerIdentityRepository,
      GoogleTokenVerifier googleTokenVerifier,
      String publicAppUrl,
      Clock clock) {
    this.otpRepository = otpRepository;
    this.otpSender = otpSender;
    this.tokenHasher = tokenHasher;
    this.loadUserByPhoneRepository = loadUserByPhoneRepository;
    this.loadUserByEmailRepository = loadUserByEmailRepository;
    this.createUserRepository = createUserRepository;
    this.customerIdentityRepository = customerIdentityRepository;
    this.googleTokenVerifier = googleTokenVerifier;
    this.publicAppUrl = publicAppUrl == null ? "" : publicAppUrl.replaceAll("/+$", "");
    this.clock = clock;
  }

  @Override
  public Options options() {
    return new Options(otpSender.available(), googleTokenVerifier.clientId());
  }

  @Override
  public int requestCode(String rawPhone) throws OtpUnavailableError, OtpTooSoonError {
    String phone = requiredPhone(rawPhone);
    if (!otpSender.available()) {
      throw new OtpUnavailableError();
    }
    Instant now = Instant.now(clock);
    Optional<OtpRepository.PendingCode> pending = otpRepository.loadPending(phone);
    if (pending.isPresent() && pending.get().createdAt().plus(RESEND_AFTER).isAfter(now)) {
      throw new OtpTooSoonError();
    }
    String code = Secrets.digits(6);
    otpRepository.save(phone, hash(phone, code), now.plus(CODE_TTL), now);
    String link =
        publicAppUrl
            + "/entrar/whatsapp?telefone="
            + URLEncoder.encode(phone, StandardCharsets.UTF_8)
            + "&codigo="
            + code;
    otpSender.send(phone, code, link);
    return (int) CODE_TTL.toSeconds();
  }

  @Override
  public User verifyCode(String rawPhone, String code, String name)
      throws InvalidOtpError, UserProfileMismatchError {
    String phone;
    try {
      phone = requiredPhone(rawPhone);
    } catch (IllegalArgumentException e) {
      throw new InvalidOtpError();
    }
    OtpRepository.PendingCode pending =
        otpRepository.loadPending(phone).orElseThrow(InvalidOtpError::new);
    Instant now = Instant.now(clock);
    if (pending.expiresAt().isBefore(now) || pending.attempts() >= MAX_ATTEMPTS) {
      otpRepository.delete(phone);
      throw new InvalidOtpError();
    }
    if (code == null
        || !MessageDigest.isEqual(
            pending.codeHash().getBytes(StandardCharsets.UTF_8),
            hash(phone, code.trim()).getBytes(StandardCharsets.UTF_8))) {
      otpRepository.incrementAttempts(pending.id());
      throw new InvalidOtpError();
    }

    Optional<User> existing = loadUserByPhoneRepository.loadByPhone(phone);
    if (existing.isPresent()) {
      User user = existing.get();
      requireCustomer(user, phone);
      otpRepository.delete(phone);
      return user;
    }
    String customerName = ContactRules.limited(name, 100, "Nome");
    if (customerName == null) {
      // Keep the code valid: the app asks for the name and verifies again.
      throw new IllegalArgumentException("Informe seu nome para criar a conta");
    }
    otpRepository.delete(phone);
    return createUserRepository.create(
        new User(UUID.randomUUID(), customerName, phone, UserProfile.CUSTOMER, now), null);
  }

  @Override
  public User signInWithGoogle(String idToken)
      throws GoogleLoginUnavailableError, InvalidCredentialsError, UserProfileMismatchError {
    if (googleTokenVerifier.clientId() == null) {
      throw new GoogleLoginUnavailableError();
    }
    GoogleTokenVerifier.GoogleIdentity identity =
        googleTokenVerifier.verify(idToken).orElseThrow(InvalidCredentialsError::new);
    Optional<User> linked = customerIdentityRepository.loadByGoogleSubject(identity.subject());
    if (linked.isPresent()) {
      requireCustomer(linked.get(), identity.email());
      return linked.get();
    }
    String email = identity.email() == null ? null : ContactRules.normalizeEmail(identity.email());
    Optional<User> byEmail =
        email == null ? Optional.empty() : loadUserByEmailRepository.loadByEmail(email);
    if (byEmail.isPresent()) {
      requireCustomer(byEmail.get(), email);
      customerIdentityRepository.linkGoogleSubject(byEmail.get().id(), identity.subject());
      return byEmail.get();
    }
    String name = ContactRules.limited(identity.name(), 100, "Nome");
    User created =
        createUserRepository.create(
            new User(
                UUID.randomUUID(),
                name == null ? "Cliente" : name,
                null,
                UserProfile.CUSTOMER,
                Instant.now(clock),
                true,
                email),
            null);
    customerIdentityRepository.linkGoogleSubject(created.id(), identity.subject());
    return created;
  }

  private static void requireCustomer(User user, String login) throws UserProfileMismatchError {
    if (user.profile() != UserProfile.CUSTOMER || !user.active()) {
      throw new UserProfileMismatchError(login, user.profile(), UserProfile.CUSTOMER);
    }
  }

  private static String requiredPhone(String raw) {
    String phone = ContactRules.normalizePhone(raw);
    if (phone == null) {
      throw new IllegalArgumentException("Informe o telefone com DDD");
    }
    return phone;
  }

  private String hash(String phone, String code) {
    return tokenHasher.hash("otp:" + phone + ":" + code);
  }
}
