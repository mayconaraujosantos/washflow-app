package com.washflow.data.usecases.user;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.TokenHasher;
import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.data.protocols.db.user.LoadUserByPhoneRepository;
import com.washflow.data.protocols.db.user.StaffCredentialsRepository;
import com.washflow.data.usecases.shared.Secrets;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.user.AccountLockedError;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.usecases.user.WasherBadge;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * A 6-digit PIN is guessable, so it only works together with the washer's phone and locks the
 * account for {@link #LOCKOUT} after {@link #MAX_FAILURES} wrong tries. The badge QR carries a
 * 256-bit random token, looked up by its keyed hash.
 */
public class DbWasherBadge implements WasherBadge {

  static final int MAX_FAILURES = 5;
  static final Duration LOCKOUT = Duration.ofMinutes(15);

  private final LoadUserByIdRepository loadUserByIdRepository;
  private final LoadUserByPhoneRepository loadUserByPhoneRepository;
  private final StaffCredentialsRepository credentials;
  private final PasswordHasher passwordHasher;
  private final TokenHasher tokenHasher;
  private final Clock clock;

  public DbWasherBadge(
      LoadUserByIdRepository loadUserByIdRepository,
      LoadUserByPhoneRepository loadUserByPhoneRepository,
      StaffCredentialsRepository credentials,
      PasswordHasher passwordHasher,
      TokenHasher tokenHasher,
      Clock clock) {
    this.loadUserByIdRepository = loadUserByIdRepository;
    this.loadUserByPhoneRepository = loadUserByPhoneRepository;
    this.credentials = credentials;
    this.passwordHasher = passwordHasher;
    this.tokenHasher = tokenHasher;
    this.clock = clock;
  }

  @Override
  public Issued issue(UUID washerId) throws UserNotFoundError {
    User user =
        loadUserByIdRepository
            .loadById(washerId)
            .orElseThrow(() -> new UserNotFoundError(washerId));
    if (user.profile() != UserProfile.WASHER) {
      throw new IllegalArgumentException("Crachá é emitido apenas para lavadores");
    }
    String pin = Secrets.digits(6);
    String badgeToken = Secrets.token(32);
    credentials.saveBadge(user.id(), passwordHasher.hash(pin), tokenHasher.hash(badgeToken));
    return new Issued(pin, badgeToken);
  }

  @Override
  public User signInWithPin(String phone, String pin)
      throws InvalidCredentialsError, AccountLockedError {
    if (phone == null || pin == null || !pin.matches("\\d{6}")) {
      throw new InvalidCredentialsError();
    }
    User user =
        loadUserByPhoneRepository
            .loadByPhone(phone.replaceAll("\\D", ""))
            .filter(found -> found.profile() == UserProfile.WASHER && found.active())
            .orElseThrow(InvalidCredentialsError::new);
    StaffCredentialsRepository.PinState state =
        credentials.loadPinState(user.id()).orElseThrow(InvalidCredentialsError::new);
    if (state.pinHash() == null) {
      throw new InvalidCredentialsError();
    }
    Instant now = Instant.now(clock);
    if (state.lockedUntil() != null && state.lockedUntil().isAfter(now)) {
      throw new AccountLockedError();
    }
    if (!passwordHasher.matches(pin, state.pinHash())) {
      int failures = state.failedAttempts() + 1;
      boolean lock = failures >= MAX_FAILURES;
      credentials.recordPinFailure(user.id(), lock ? 0 : failures, lock ? now.plus(LOCKOUT) : null);
      if (lock) {
        throw new AccountLockedError();
      }
      throw new InvalidCredentialsError();
    }
    credentials.resetPinFailures(user.id());
    return user;
  }

  @Override
  public User signInWithBadge(String badgeToken) throws InvalidCredentialsError {
    if (badgeToken == null || badgeToken.isBlank()) {
      throw new InvalidCredentialsError();
    }
    return credentials
        .loadByBadgeHash(tokenHasher.hash(badgeToken.trim()))
        .filter(user -> user.profile() == UserProfile.WASHER && user.active())
        .orElseThrow(InvalidCredentialsError::new);
  }
}
