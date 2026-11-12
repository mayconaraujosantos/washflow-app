package com.washflow.data.usecases.customer;

import com.washflow.data.protocols.TokenHasher;
import com.washflow.data.protocols.db.customer.CustomerIdentityRepository;
import com.washflow.data.protocols.db.customer.OtpRepository;
import com.washflow.data.protocols.db.user.CreateUserRepository;
import com.washflow.data.protocols.db.user.LoadUserByPhoneRepository;
import com.washflow.data.protocols.identity.GoogleTokenVerifier;
import com.washflow.data.protocols.messaging.OtpSender;
import com.washflow.data.usecases.shared.Secrets;
import com.washflow.domain.entities.customer.OtpRecipient;
import com.washflow.domain.entities.shared.ContactRules;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.auth.GoogleLoginUnavailableError;
import com.washflow.domain.errors.auth.InvalidOtpError;
import com.washflow.domain.errors.auth.OtpTooSoonError;
import com.washflow.domain.errors.auth.OtpUnavailableError;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.errors.user.UserProfileMismatchError;
import com.washflow.domain.usecases.customer.CustomerPasswordlessLogin;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * The code goes to a phone (WhatsApp) or an e-mail address, is stored only as a keyed hash, expires
 * after {@link #CODE_TTL}, allows {@link #MAX_ATTEMPTS} guesses and can be re-sent after {@link
 * #RESEND_AFTER}. The request answer is the same whether the recipient has an account or not.
 */
@SuppressWarnings("java:S107") // one port per collaborator, like the other Db* use cases
public class DbCustomerPasswordlessLogin implements CustomerPasswordlessLogin {

  static final Duration CODE_TTL = Duration.ofMinutes(10);
  static final Duration RESEND_AFTER = Duration.ofSeconds(60);
  static final int MAX_ATTEMPTS = 5;

  private final OtpRepository otpRepository;
  private final OtpSender whatsappSender;
  private final OtpSender emailSender;
  private final TokenHasher tokenHasher;
  private final LoadUserByPhoneRepository loadUserByPhoneRepository;
  private final CreateUserRepository createUserRepository;
  private final CustomerIdentityRepository customerIdentityRepository;
  private final GoogleTokenVerifier googleTokenVerifier;
  private final String publicAppUrl;
  private final Clock clock;

  public DbCustomerPasswordlessLogin(
      OtpRepository otpRepository,
      OtpSender whatsappSender,
      OtpSender emailSender,
      TokenHasher tokenHasher,
      LoadUserByPhoneRepository loadUserByPhoneRepository,
      CreateUserRepository createUserRepository,
      CustomerIdentityRepository customerIdentityRepository,
      GoogleTokenVerifier googleTokenVerifier,
      String publicAppUrl,
      Clock clock) {
    this.otpRepository = otpRepository;
    this.whatsappSender = whatsappSender;
    this.emailSender = emailSender;
    this.tokenHasher = tokenHasher;
    this.loadUserByPhoneRepository = loadUserByPhoneRepository;
    this.createUserRepository = createUserRepository;
    this.customerIdentityRepository = customerIdentityRepository;
    this.googleTokenVerifier = googleTokenVerifier;
    this.publicAppUrl = withoutTrailingSlashes(publicAppUrl);
    this.clock = clock;
  }

  @Override
  public Options options() {
    return new Options(
        whatsappSender.available(), emailSender.available(), googleTokenVerifier.clientId());
  }

  @Override
  public int requestCode(OtpRecipient recipient) {
    OtpSender sender = sender(recipient);
    if (!sender.available()) {
      throw unavailable(recipient);
    }
    Instant now = Instant.now(clock);
    Optional<OtpRepository.PendingCode> pending = otpRepository.loadPending(recipient);
    if (pending.isPresent() && pending.get().createdAt().plus(RESEND_AFTER).isAfter(now)) {
      throw new OtpTooSoonError();
    }
    String code = Secrets.digits(6);
    otpRepository.save(recipient, hash(recipient, code), now.plus(CODE_TTL), now);
    try {
      sender.send(recipient.address(), code, magicLink(recipient, code));
    } catch (OtpUnavailableError e) {
      // Nothing was delivered: let the customer ask again right away.
      otpRepository.delete(recipient);
      throw e;
    }
    return (int) CODE_TTL.toSeconds();
  }

  @Override
  public User verifyCode(OtpRecipient recipient, String code, String name) {
    OtpRepository.PendingCode pending =
        otpRepository.loadPending(recipient).orElseThrow(InvalidOtpError::new);
    Instant now = Instant.now(clock);
    if (pending.expiresAt().isBefore(now) || pending.attempts() >= MAX_ATTEMPTS) {
      otpRepository.delete(recipient);
      throw new InvalidOtpError();
    }
    if (code == null
        || !MessageDigest.isEqual(
            pending.codeHash().getBytes(StandardCharsets.UTF_8),
            hash(recipient, code.trim()).getBytes(StandardCharsets.UTF_8))) {
      otpRepository.incrementAttempts(pending.id());
      throw new InvalidOtpError();
    }

    Optional<User> existing =
        recipient.isEmail()
            ? customerIdentityRepository.loadByEmail(recipient.address())
            : loadUserByPhoneRepository.loadByPhone(recipient.address());
    if (existing.isPresent()) {
      User user = existing.get();
      requireCustomer(user, recipient.address());
      otpRepository.delete(recipient);
      return user;
    }
    String customerName = ContactRules.limited(name, 100, "Nome");
    if (customerName == null) {
      // Keep the code valid: the app asks for the name and verifies again.
      throw new IllegalArgumentException("Informe seu nome para criar a conta");
    }
    otpRepository.delete(recipient);
    return createUserRepository.create(
        new User(
            UUID.randomUUID(),
            customerName,
            recipient.isEmail() ? null : recipient.address(),
            UserProfile.CUSTOMER,
            now,
            true,
            recipient.isEmail() ? recipient.address() : null),
        null);
  }

  @Override
  public User signInWithGoogle(String idToken) {
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
        email == null ? Optional.empty() : customerIdentityRepository.loadByEmail(email);
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

  private static void requireCustomer(User user, String login) {
    if (user.profile() != UserProfile.CUSTOMER || !user.active()) {
      throw new UserProfileMismatchError(login, user.profile(), UserProfile.CUSTOMER);
    }
  }

  private OtpSender sender(OtpRecipient recipient) {
    return recipient.isEmail() ? emailSender : whatsappSender;
  }

  private static OtpUnavailableError unavailable(OtpRecipient recipient) {
    return recipient.isEmail()
        ? new OtpUnavailableError("Envio de código por e-mail não está configurado")
        : new OtpUnavailableError();
  }

  private String magicLink(OtpRecipient recipient, String code) {
    String target = recipient.isEmail() ? "/entrar/email?email=" : "/entrar/whatsapp?telefone=";
    return publicAppUrl
        + target
        + URLEncoder.encode(recipient.address(), StandardCharsets.UTF_8)
        + "&codigo="
        + code;
  }

  private String hash(OtpRecipient recipient, String code) {
    // Phone codes keep their original key; e-mail ones are prefixed so the two never collide.
    String key = recipient.isEmail() ? "email:" + recipient.address() : recipient.address();
    return tokenHasher.hash("otp:" + key + ":" + code);
  }

  private static String withoutTrailingSlashes(String url) {
    if (url == null) {
      return "";
    }
    int end = url.length();
    while (end > 0 && url.charAt(end - 1) == '/') {
      end--;
    }
    return url.substring(0, end);
  }
}
