package com.washflow.data.usecases.user;

import static java.util.stream.Collectors.partitioningBy;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.TokenHasher;
import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.data.protocols.db.user.LoadWashersByPhoneRepository;
import com.washflow.data.protocols.db.user.StaffCredentialsRepository;
import com.washflow.data.protocols.db.user.StaffCredentialsRepository.PinState;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A 6-digit PIN is guessable, so it only works together with the washer's phone and locks the
 * account for {@link #LOCKOUT} after {@link #MAX_FAILURES} wrong tries. The badge QR carries a
 * 256-bit random token, looked up by its keyed hash.
 */
public class DbWasherBadge implements WasherBadge {

  static final int MAX_FAILURES = 5;
  static final Duration LOCKOUT = Duration.ofMinutes(15);

  private static final int PIN_DIGITS = 6;
  private static final int BADGE_TOKEN_BYTES = 32;
  private static final Pattern PIN_FORMAT = Pattern.compile("\\d{" + PIN_DIGITS + "}");
  private static final Pattern NON_DIGIT = Pattern.compile("\\D");

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
  public Issued issue(UUID washerId) {
    User user =
        loadUserByIdRepository
            .loadById(washerId)
            .orElseThrow(() -> new UserNotFoundError(washerId));
    if (user.profile() != UserProfile.WASHER) {
      throw new IllegalArgumentException("Crachá é emitido apenas para lavadores");
    }
    String pin = Secrets.digits(PIN_DIGITS);
    String badgeToken = Secrets.token(BADGE_TOKEN_BYTES);
    credentials.saveBadge(user.id(), passwordHasher.hash(pin), tokenHasher.hash(badgeToken));
    return new Issued(pin, badgeToken);
  }

  /**
   * The phone may belong to washer accounts in more than one lava-jato (same person, two jobs): the
   * PIN is checked against each badge and signs into the only account whose badge it matches. Every
   * badge that did not match counts the failure towards its own lockout.
   */
  @Override
  public User signInWithPin(String phone, String pin) {
    if (phone == null || pin == null || !PIN_FORMAT.matcher(pin).matches()) {
      throw new InvalidCredentialsError();
    }
    Instant now = Instant.now(clock);
    List<Badge> badges = badgesOnPhone(phone);
    List<Badge> open = badges.stream().filter(badge -> !badge.isLockedAt(now)).toList();
    Map<Boolean, List<Badge>> byPin =
        open.stream()
            .collect(partitioningBy(badge -> passwordHasher.matches(pin, badge.state().pinHash())));
    List<Badge> matched = byPin.get(true);
    if (matched.size() == 1) {
      User washer = matched.getFirst().washer();
      credentials.resetPinFailures(washer.id());
      return washer;
    }
    boolean anyStillOpen = recordFailures(byPin.get(false), now);
    if (matched.isEmpty() && !anyStillOpen && !badges.isEmpty()) {
      // Every badge on this phone is now locked (or already was).
      throw new AccountLockedError();
    }
    // No badge, a wrong PIN, or (vanishingly rare) the same PIN on two badges: the QR still works.
    throw new InvalidCredentialsError();
  }

  @Override
  public User signInWithBadge(String badgeToken) {
    if (badgeToken == null || badgeToken.isBlank()) {
      throw new InvalidCredentialsError();
    }
    return credentials
        .loadByBadgeHash(tokenHasher.hash(badgeToken.trim()))
        .filter(user -> user.profile() == UserProfile.WASHER && user.active())
        .orElseThrow(InvalidCredentialsError::new);
  }

  /** Active washer accounts on this phone that have a PIN, in the repository's order. */
  private List<Badge> badgesOnPhone(String phone) {
    String digits = NON_DIGIT.matcher(phone).replaceAll("");
    return loadWashersByPhoneRepository.loadWashersByPhone(digits).stream()
        .filter(User::active)
        .flatMap(
            washer ->
                credentials
                    .loadPinState(washer.id())
                    .filter(state -> state.pinHash() != null)
                    .map(state -> new Badge(washer, state))
                    .stream())
        .toList();
  }

  /** Counts a wrong PIN on each badge; returns whether any of them is still unlocked. */
  private boolean recordFailures(List<Badge> missed, Instant now) {
    boolean anyStillOpen = false;
    for (Badge badge : missed) {
      int failures = badge.state().failedAttempts() + 1;
      boolean lock = failures >= MAX_FAILURES;
      credentials.recordPinFailure(
          badge.washer().id(), lock ? 0 : failures, lock ? now.plus(LOCKOUT) : null);
      anyStillOpen |= !lock;
    }
    return anyStillOpen;
  }

  /** A washer account together with its PIN state. */
  private record Badge(User washer, PinState state) {

    boolean isLockedAt(Instant now) {
      return state.lockedUntil() != null && state.lockedUntil().isAfter(now);
    }
  }
}
