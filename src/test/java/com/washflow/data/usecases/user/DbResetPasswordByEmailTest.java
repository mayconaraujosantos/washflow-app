package com.washflow.data.usecases.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.db.user.LoadUserByEmailRepository;
import com.washflow.data.protocols.db.user.LoadUserByPhoneRepository;
import com.washflow.data.protocols.db.user.PasswordResetRepository;
import com.washflow.data.protocols.db.user.PasswordResetRepository.PendingCode;
import com.washflow.data.protocols.db.user.SaveUserPasswordHashRepository;
import com.washflow.data.protocols.messaging.EmailSender;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.auth.InvalidOtpError;
import com.washflow.domain.errors.user.EmailDeliveryFailedError;
import com.washflow.domain.errors.user.EmailResetUnavailableError;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DbResetPasswordByEmailTest {

  private static final Instant NOW = Instant.parse("2026-11-10T15:00:00Z");
  private static final String PHONE = "92991234567";

  /** Keeps what was "sent" so the test can read the code like the person would. */
  private static final class Outbox implements EmailSender {
    final List<String[]> sent = new ArrayList<>();
    boolean up = true;
    boolean configured = true;

    @Override
    public void send(String to, String subject, String body) {
      if (!up) {
        throw new EmailDeliveryException("smtp down", null);
      }
      sent.add(new String[] {to, subject, body});
    }

    @Override
    public boolean available() {
      return configured;
    }

    String lastCode() {
      Matcher matcher = Pattern.compile("\\b(\\d{6})\\b").matcher(sent.getLast()[2]);
      assertTrue(matcher.find(), "code in the e-mail body");
      return matcher.group(1);
    }
  }

  /** In-memory code store with the same semantics as the JDBI one. */
  private static final class Codes {
    PendingCode code;
  }

  private final LoadUserByPhoneRepository byPhone = mock(LoadUserByPhoneRepository.class);
  private final LoadUserByEmailRepository byEmail = mock(LoadUserByEmailRepository.class);
  private final PasswordResetRepository resets = mock(PasswordResetRepository.class);
  private final SaveUserPasswordHashRepository passwords =
      mock(SaveUserPasswordHashRepository.class);
  private final PasswordHasher hasher = mock(PasswordHasher.class);
  private final Outbox outbox = new Outbox();
  private final Codes codes = new Codes();
  private Instant now = NOW;
  private DbResetPasswordByEmail sut;

  private final User ana =
      new User(UUID.randomUUID(), "Ana Cliente", PHONE, UserProfile.CUSTOMER, NOW, true);
  private final User rita =
      new User(
          UUID.randomUUID(), "Rita", null, UserProfile.MANAGER, NOW, true, "rita@lavaraio.test");

  @BeforeEach
  void setUp() {
    Clock clock =
        new Clock() {
          @Override
          public ZoneOffset getZone() {
            return ZoneOffset.UTC;
          }

          @Override
          public Clock withZone(java.time.ZoneId zone) {
            return this;
          }

          @Override
          public Instant instant() {
            return now;
          }
        };
    sut =
        new DbResetPasswordByEmail(
            byPhone,
            byEmail,
            resets,
            passwords,
            hasher,
            outbox,
            new PasswordResetCodes(resets, value -> "h:" + value, clock));
    when(byPhone.loadByPhone(PHONE)).thenReturn(Optional.of(ana));
    when(byEmail.loadByEmail("rita@lavaraio.test")).thenReturn(Optional.of(rita));
    when(resets.loadRecoveryEmail(ana.id())).thenReturn(Optional.of("ana@mail.test"));
    when(hasher.hash(anyString())).thenAnswer(call -> "bcrypt:" + call.getArgument(0));
    // Code store behaves like the table: one row per account.
    org.mockito.Mockito.doAnswer(
            call -> {
              codes.code =
                  new PendingCode(call.getArgument(1), 0, call.getArgument(2), call.getArgument(3));
              return null;
            })
        .when(resets)
        .saveCode(any(), anyString(), any(), any());
    when(resets.loadCode(any())).thenAnswer(call -> Optional.ofNullable(codes.code));
    org.mockito.Mockito.doAnswer(
            call -> {
              codes.code =
                  new PendingCode(
                      codes.code.codeHash(),
                      codes.code.attempts() + 1,
                      codes.code.expiresAt(),
                      codes.code.createdAt());
              return null;
            })
        .when(resets)
        .incrementAttempts(any());
    org.mockito.Mockito.doAnswer(
            call -> {
              codes.code = null;
              return null;
            })
        .when(resets)
        .deleteCode(any());
  }

  @Test
  void customerGetsTheCodeByEmailAndSetsANewPassword() throws Exception {
    assertEquals(600, sut.requestForCustomer("(92) 99123-4567"));

    assertEquals("ana@mail.test", outbox.sent.getLast()[0]);
    assertTrue(outbox.sent.getLast()[2].startsWith("Olá, Ana!"));
    sut.confirmForCustomer(PHONE, outbox.lastCode(), "senha-nova-456");

    verify(passwords).savePasswordHash(ana.id(), "bcrypt:senha-nova-456");
    assertEquals(null, codes.code, "a used code cannot be used again");
  }

  @Test
  void managerGetsTheCodeAtTheLoginEmail() throws Exception {
    sut.requestForManager("Rita@LavaRaio.test");

    assertEquals("rita@lavaraio.test", outbox.sent.getLast()[0]);
    sut.confirmForManager("rita@lavaraio.test", outbox.lastCode(), "senha-nova-456");
    verify(passwords).savePasswordHash(rita.id(), "bcrypt:senha-nova-456");
  }

  @Test
  void answersTheSameWithoutRevealingWhetherTheAccountExists() throws Exception {
    when(byPhone.loadByPhone("92900000000")).thenReturn(Optional.empty());
    when(byEmail.loadByEmail("ninguem@x.test")).thenReturn(Optional.empty());
    User noEmail = new User(UUID.randomUUID(), "Bia", "92911112222", UserProfile.CUSTOMER, NOW);
    when(byPhone.loadByPhone("92911112222")).thenReturn(Optional.of(noEmail));
    when(resets.loadRecoveryEmail(noEmail.id())).thenReturn(Optional.empty());

    assertEquals(600, sut.requestForCustomer("92900000000"));
    assertEquals(600, sut.requestForManager("ninguem@x.test"));
    assertEquals(600, sut.requestForCustomer("92911112222"));
    // a customer's phone is not a manager login, and vice versa
    assertEquals(600, sut.requestForManager("ana@mail.test"));

    assertTrue(outbox.sent.isEmpty());
  }

  @Test
  void resendsOnlyAfterAMinuteAndTheNewCodeReplacesTheOld() throws Exception {
    sut.requestForCustomer(PHONE);
    String first = outbox.lastCode();
    sut.requestForCustomer(PHONE);
    assertEquals(1, outbox.sent.size(), "asked again too soon: same answer, no new e-mail");

    now = NOW.plusSeconds(61);
    sut.requestForCustomer(PHONE);
    assertEquals(2, outbox.sent.size());
    String second = outbox.lastCode();
    if (!first.equals(second)) {
      assertThrows(
          InvalidOtpError.class, () -> sut.confirmForCustomer(PHONE, first, "senha-nova-456"));
    }
    sut.confirmForCustomer(PHONE, second, "senha-nova-456");
  }

  @Test
  void fiveWrongCodesOrTenMinutesEndTheCode() throws Exception {
    sut.requestForCustomer(PHONE);
    String code = outbox.lastCode();
    String wrong = "000000".equals(code) ? "111111" : "000000";
    for (int i = 0; i < 5; i++) {
      assertThrows(
          InvalidOtpError.class, () -> sut.confirmForCustomer(PHONE, wrong, "senha-nova-456"));
    }
    assertThrows(
        InvalidOtpError.class, () -> sut.confirmForCustomer(PHONE, code, "senha-nova-456"));

    now = NOW.plusSeconds(120);
    sut.requestForCustomer(PHONE);
    String fresh = outbox.lastCode();
    now = now.plusSeconds(601);
    assertThrows(
        InvalidOtpError.class, () -> sut.confirmForCustomer(PHONE, fresh, "senha-nova-456"));
    verify(passwords, never()).savePasswordHash(any(), any());
  }

  @Test
  void refusesShortPasswordsAndUnknownAccountsOnConfirm() {
    assertThrows(
        IllegalArgumentException.class, () -> sut.confirmForCustomer(PHONE, "123456", "curta"));
    when(byEmail.loadByEmail("ninguem@x.test")).thenReturn(Optional.empty());
    assertThrows(
        InvalidOtpError.class,
        () -> sut.confirmForManager("ninguem@x.test", "123456", "senha-nova-456"));
    assertThrows(
        InvalidOtpError.class, () -> sut.confirmForCustomer(PHONE, "123456", "senha-nova-456"));
  }

  @Test
  void reportsAMissingChannelAndAFailedDelivery() {
    outbox.configured = false;
    assertThrows(EmailResetUnavailableError.class, () -> sut.requestForCustomer(PHONE));

    outbox.configured = true;
    outbox.up = false;
    assertThrows(EmailDeliveryFailedError.class, () -> sut.requestForCustomer(PHONE));
    assertEquals(null, codes.code, "an e-mail that never left leaves no pending code");
    verify(resets).deleteCode(eq(ana.id()));
  }

  @Test
  void rejectsMalformedPhoneOrEmailOnRequest() {
    assertThrows(IllegalArgumentException.class, () -> sut.requestForCustomer(""));
    assertThrows(IllegalArgumentException.class, () -> sut.requestForManager("sem-arroba"));
  }
}
