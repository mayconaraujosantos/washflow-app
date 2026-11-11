package com.washflow.data.usecases.user;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.TokenHasher;
import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.data.protocols.db.user.LoadWashersByPhoneRepository;
import com.washflow.data.protocols.db.user.StaffCredentialsRepository;
import com.washflow.data.usecases.shared.Secrets;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.user.AccountLockedError;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.usecases.user.WasherBadge;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
  private final LoadWashersByPhoneRepository loadWashersByPhoneRepository;
  private final StaffCredentialsRepository credentials;
  private final PasswordHasher passwordHasher;
  private final TokenHasher tokenHasher;
  private final Clock clock;

  public DbWasherBadge(
      LoadUserByIdRepository loadUserByIdRepository,
      LoadWashersByPhoneRepository loadWashersByPhoneRepository,
      StaffCredentialsRepository credentials,
      PasswordHasher passwordHasher,
      TokenHasher tokenHasher,
      Clock clock) {
    this.loadUserByIdRepository = loadUserByIdRepository;
    this.loadWashersByPhoneRepository = loadWashersByPhoneRepository;
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

  /**
   * The phone may belong to washer accounts in more than one lava-jato (same person, two jobs): the
   * PIN is checked against each one and signs into the only account whose badge it matches. Every
   * account that did not match counts the failure towards its own lockout.
   */
  @Override
  public User signInWithPin(String phone, String pin)
      throws InvalidCredentialsError, AccountLockedError {
    if (phone == null || pin == null || !pin.matches("\\d{6}")) {
      throw new InvalidCredentialsError();
    }
    Instant now = Instant.now(clock);
    List<User> matched = new ArrayList<>();
    Map<UUID, StaffCredentialsRepository.PinState> missed = new LinkedHashMap<>();
    boolean lockedOut = false;
    for (User washer :
        loadWashersByPhoneRepository.loadWashersByPhone(phone.replaceAll("\\D", ""))) {
      if (!washer.active()) {
        continue;
      }
      var state = credentials.loadPinState(washer.id()).orElse(null);
      if (state == null || state.pinHash() == null) {
        continue;
      }
      if (state.lockedUntil() != null && state.lockedUntil().isAfter(now)) {
        lockedOut = true;
      } else if (passwordHasher.matches(pin, state.pinHash())) {
        matched.add(washer);
      } else {
        missed.put(washer.id(), state);
      }
    }
    if (matched.size() == 1) {
      credentials.resetPinFailures(matched.get(0).id());
      return matched.get(0);
    }
    boolean anyStillOpen = false;
    for (var entry : missed.entrySet()) {
      int failures = entry.getValue().failedAttempts() + 1;
      boolean lock = failures >= MAX_FAILURES;
      credentials.recordPinFailure(
          entry.getKey(), lock ? 0 : failures, lock ? now.plus(LOCKOUT) : null);
      anyStillOpen |= !lock;
    }
    // Every badge on this phone is now locked (or already was).
    if (matched.isEmpty() && !anyStillOpen && (lockedOut || !missed.isEmpty())) {
      throw new AccountLockedError();
    }
    // No match, or (vanishingly rare) the same PIN on two badges: the QR still works.
    throw new InvalidCredentialsError();
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
