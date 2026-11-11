package com.washflow.data.usecases.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.TokenHasher;
import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.data.protocols.db.user.LoadWashersByPhoneRepository;
import com.washflow.data.protocols.db.user.StaffCredentialsRepository;
import com.washflow.data.protocols.db.user.StaffCredentialsRepository.PinState;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.user.AccountLockedError;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.usecases.user.WasherBadge;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbWasherBadgeTest {

  private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");
  private static final String PHONE = "92999990002";
  private static final String PIN = "123456";

  /** "Hashes" by prefixing, so a PIN matches only its own hash. */
  private static final PasswordHasher HASHER =
      new PasswordHasher() {
        @Override
        public String hash(String password) {
          return "h:" + password;
        }

        @Override
        public boolean matches(String password, String passwordHash) {
          return passwordHash.equals(hash(password));
        }
      };

  private static final TokenHasher TOKEN_HASHER = value -> "t:" + value;

  @Mock private LoadUserByIdRepository loadUserById;
  @Mock private LoadWashersByPhoneRepository loadWashersByPhone;
  @Mock private StaffCredentialsRepository credentials;

  private DbWasherBadge sut;

  @BeforeEach
  void setUp() {
    sut =
        new DbWasherBadge(
            loadUserById,
            loadWashersByPhone,
            credentials,
            HASHER,
            TOKEN_HASHER,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static User washer(boolean active) {
    return new User(UUID.randomUUID(), "Lavador", PHONE, UserProfile.WASHER, NOW, active);
  }

  private static PinState pin(String pin, int failures) {
    return new PinState(HASHER.hash(pin), failures, null);
  }

  private void given(User washer, PinState state) {
    when(credentials.loadPinState(washer.id())).thenReturn(Optional.ofNullable(state));
  }

  @Test
  void issuesASixDigitPinAndABadgeTokenAndStoresOnlyTheirHashes() throws Exception {
    User washer = washer(true);
    when(loadUserById.loadById(washer.id())).thenReturn(Optional.of(washer));

    WasherBadge.Issued issued = sut.issue(washer.id());

    assertTrue(issued.pin().matches("\\d{6}"));
    assertTrue(issued.badgeToken().length() >= 32);
    verify(credentials)
        .saveBadge(washer.id(), HASHER.hash(issued.pin()), TOKEN_HASHER.hash(issued.badgeToken()));
  }

  @Test
  void refusesToIssueABadgeForAnUnknownOrNonWasherAccount() {
    UUID unknown = UUID.randomUUID();
    when(loadUserById.loadById(unknown)).thenReturn(Optional.empty());
    User customer = new User(UUID.randomUUID(), "Bia", PHONE, UserProfile.CUSTOMER, NOW, true);
    when(loadUserById.loadById(customer.id())).thenReturn(Optional.of(customer));

    assertThrows(UserNotFoundError.class, () -> sut.issue(unknown));
    assertThrows(IllegalArgumentException.class, () -> sut.issue(customer.id()));
    verify(credentials, never()).saveBadge(any(), any(), any());
  }

  @Test
  void rejectsMissingPhoneOrMalformedPinWithoutTouchingTheDatabase() {
    assertThrows(InvalidCredentialsError.class, () -> sut.signInWithPin(null, PIN));
    assertThrows(InvalidCredentialsError.class, () -> sut.signInWithPin(PHONE, null));
    assertThrows(InvalidCredentialsError.class, () -> sut.signInWithPin(PHONE, "12345"));
    assertThrows(InvalidCredentialsError.class, () -> sut.signInWithPin(PHONE, "12345a"));
    verify(loadWashersByPhone, never()).loadWashersByPhone(any());
  }

  @Test
  void signsInWithTheRightPinUsingOnlyThePhoneDigitsAndClearsFailures() throws Exception {
    User washer = washer(true);
    when(loadWashersByPhone.loadWashersByPhone(PHONE)).thenReturn(List.of(washer));
    given(washer, pin(PIN, 3));

    assertEquals(washer, sut.signInWithPin("(92) 99999-0002", PIN));
    verify(credentials).resetPinFailures(washer.id());
    verify(credentials, never()).recordPinFailure(any(), anyInt(), any());
  }

  @Test
  void countsAWrongPinAndLocksOnTheFifthFailure() {
    User washer = washer(true);
    when(loadWashersByPhone.loadWashersByPhone(PHONE)).thenReturn(List.of(washer));
    given(washer, pin(PIN, 0));

    assertThrows(InvalidCredentialsError.class, () -> sut.signInWithPin(PHONE, "000000"));
    verify(credentials).recordPinFailure(eq(washer.id()), eq(1), isNull());

    given(washer, pin(PIN, DbWasherBadge.MAX_FAILURES - 1));
    assertThrows(AccountLockedError.class, () -> sut.signInWithPin(PHONE, "000000"));
    verify(credentials).recordPinFailure(washer.id(), 0, NOW.plus(DbWasherBadge.LOCKOUT));
  }

  @Test
  void aLockedBadgeRefusesEvenTheRightPinUntilTheLockoutEnds() throws Exception {
    User washer = washer(true);
    when(loadWashersByPhone.loadWashersByPhone(PHONE)).thenReturn(List.of(washer));
    given(washer, new PinState(HASHER.hash(PIN), 0, NOW.plusSeconds(60)));

    assertThrows(AccountLockedError.class, () -> sut.signInWithPin(PHONE, PIN));
    verify(credentials, never()).resetPinFailures(any());

    given(washer, new PinState(HASHER.hash(PIN), 0, NOW));
    assertEquals(washer, sut.signInWithPin(PHONE, PIN));
  }

  @Test
  void skipsInactiveAccountsAndAccountsWithoutABadge() {
    User inactive = washer(false);
    User withoutBadge = washer(true);
    when(loadWashersByPhone.loadWashersByPhone(PHONE)).thenReturn(List.of(inactive, withoutBadge));
    given(withoutBadge, null);

    assertThrows(InvalidCredentialsError.class, () -> sut.signInWithPin(PHONE, PIN));
    verify(credentials, never()).loadPinState(inactive.id());
    verify(credentials, never()).recordPinFailure(any(), anyInt(), any());
  }

  @Test
  void withTheSamePhoneInTwoLavaJatosThePinOpensOnlyItsOwnAccount() throws Exception {
    User first = washer(true);
    User second = washer(true);
    when(loadWashersByPhone.loadWashersByPhone(PHONE)).thenReturn(List.of(first, second));
    given(first, pin("111111", 0));
    given(second, pin(PIN, 2));

    assertEquals(second, sut.signInWithPin(PHONE, PIN));
    verify(credentials).resetPinFailures(second.id());
    verify(credentials, never()).recordPinFailure(any(), anyInt(), any());
  }

  @Test
  void aWrongPinIsInvalidWhileAnotherBadgeOnThePhoneIsStillOpen() {
    User locked = washer(true);
    User open = washer(true);
    when(loadWashersByPhone.loadWashersByPhone(PHONE)).thenReturn(List.of(locked, open));
    given(locked, new PinState(HASHER.hash("111111"), 0, NOW.plusSeconds(60)));
    given(open, pin(PIN, 0));

    assertThrows(InvalidCredentialsError.class, () -> sut.signInWithPin(PHONE, "000000"));
    verify(credentials).recordPinFailure(eq(open.id()), eq(1), isNull());
    verify(credentials, never()).recordPinFailure(eq(locked.id()), anyInt(), any());
  }

  @Test
  void theSamePinOnTwoBadgesSignsIntoNeither() {
    User first = washer(true);
    User second = washer(true);
    when(loadWashersByPhone.loadWashersByPhone(PHONE)).thenReturn(List.of(first, second));
    given(first, pin(PIN, 0));
    given(second, pin(PIN, 0));

    assertThrows(InvalidCredentialsError.class, () -> sut.signInWithPin(PHONE, PIN));
    verify(credentials, never()).resetPinFailures(any());
  }

  @Test
  void signsInWithTheBadgeTokenOfAnActiveWasherOnly() throws Exception {
    User washer = washer(true);
    when(credentials.loadByBadgeHash(TOKEN_HASHER.hash("token"))).thenReturn(Optional.of(washer));
    User inactive = washer(false);
    when(credentials.loadByBadgeHash(TOKEN_HASHER.hash("old"))).thenReturn(Optional.of(inactive));

    assertEquals(washer, sut.signInWithBadge("  token "));
    assertThrows(InvalidCredentialsError.class, () -> sut.signInWithBadge("old"));
    assertThrows(InvalidCredentialsError.class, () -> sut.signInWithBadge(" "));
    assertThrows(InvalidCredentialsError.class, () -> sut.signInWithBadge(null));
  }
}
