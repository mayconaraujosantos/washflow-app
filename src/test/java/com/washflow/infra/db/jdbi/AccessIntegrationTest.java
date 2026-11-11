package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.data.protocols.identity.GoogleTokenVerifier;
import com.washflow.data.protocols.messaging.OtpSender;
import com.washflow.data.usecases.customer.DbCustomerPasswordlessLogin;
import com.washflow.data.usecases.customer.DbServiceOrderTicket;
import com.washflow.data.usecases.user.DbWasherBadge;
import com.washflow.domain.entities.customer.OtpRecipient;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.domain.entities.serviceorder.TicketView;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.errors.auth.InvalidOtpError;
import com.washflow.domain.errors.auth.OtpTooSoonError;
import com.washflow.domain.errors.auth.OtpUnavailableError;
import com.washflow.domain.errors.common.TicketNotFoundError;
import com.washflow.domain.errors.user.AccountLockedError;
import com.washflow.domain.errors.user.InvalidCredentialsError;
import com.washflow.domain.errors.user.UserProfileMismatchError;
import com.washflow.domain.usecases.user.WasherBadge;
import com.washflow.infra.db.jdbi.customer.AccessJdbiRepository;
import com.washflow.infra.db.jdbi.user.UserJdbiRepository;
import com.washflow.infra.messaging.UnavailableOtpSender;
import com.washflow.infra.security.BCryptPasswordHasher;
import com.washflow.infra.security.HmacTokenHasher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

class AccessIntegrationTest {

  private static final UUID DEMO_WASHER = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final String DEMO_WASHER_PHONE = "11999991111";
  private static final HmacTokenHasher HASHER = new HmacTokenHasher("test-secret-0123456789abcdef");
  private static final OtpRecipient PHONE = OtpRecipient.phone("92991234567");

  @Test
  void managerIssuesABadgeAndTheWasherSignsInWithPinOrQr() throws Exception {
    Jdbi jdbi = createJdbi();
    UserJdbiRepository users = new UserJdbiRepository(jdbi);
    DbWasherBadge badge =
        new DbWasherBadge(
            users,
            users,
            new AccessJdbiRepository(jdbi),
            new BCryptPasswordHasher(),
            HASHER,
            Clock.systemUTC());

    WasherBadge.Issued issued = badge.issue(DEMO_WASHER);

    assertTrue(issued.pin().matches("\\d{6}"));
    assertEquals(DEMO_WASHER, badge.signInWithPin("(11) 99999-1111", issued.pin()).id());
    assertEquals(DEMO_WASHER, badge.signInWithBadge(issued.badgeToken()).id());

    WasherBadge.Issued reissued = badge.issue(DEMO_WASHER);
    assertThrows(InvalidCredentialsError.class, () -> badge.signInWithBadge(issued.badgeToken()));
    assertEquals(DEMO_WASHER, badge.signInWithBadge(reissued.badgeToken()).id());
    assertThrows(
        IllegalArgumentException.class,
        () -> badge.issue(UUID.fromString("44444444-4444-4444-4444-444444444444")));
  }

  @Test
  void fiveWrongPinsLockTheWasherEvenForTheRightPin() throws Exception {
    Jdbi jdbi = createJdbi();
    UserJdbiRepository users = new UserJdbiRepository(jdbi);
    DbWasherBadge badge =
        new DbWasherBadge(
            users,
            users,
            new AccessJdbiRepository(jdbi),
            new BCryptPasswordHasher(),
            HASHER,
            Clock.systemUTC());
    String pin = badge.issue(DEMO_WASHER).pin();
    String wrong = pin.equals("000000") ? "111111" : "000000";

    for (int i = 0; i < 4; i++) {
      assertThrows(
          InvalidCredentialsError.class, () -> badge.signInWithPin(DEMO_WASHER_PHONE, wrong));
    }
    assertThrows(AccountLockedError.class, () -> badge.signInWithPin(DEMO_WASHER_PHONE, wrong));
    assertThrows(AccountLockedError.class, () -> badge.signInWithPin(DEMO_WASHER_PHONE, pin));
  }

  @Test
  void ticketIsStableAndShowsProgressWithoutCustomerData() throws Exception {
    Jdbi jdbi = createJdbi();
    UUID orderId = UUID.randomUUID();
    jdbi.useHandle(
        handle ->
            handle
                .createUpdate(
                    """
                    INSERT INTO atendimentos
                      (id, estabelecimento_id, veiculo_id, servico_preco_id, status, data_agendamento,
                       criado_em, atualizado_em)
                    VALUES (:id, :tenant, '22222222-2222-2222-2222-222222222222', 1, 'EM_LAVAGEM',
                            :now, :now, :now)
                    """)
                .bind("id", orderId)
                .bind("now", Instant.now())
                .execute());
    DbServiceOrderTicket ticket = new DbServiceOrderTicket(new AccessJdbiRepository(jdbi));

    String token = ticket.issue(orderId);
    assertEquals(token, ticket.issue(orderId), "re-issuing keeps the printed QR valid");
    TicketView view = ticket.view(token);

    assertEquals("ABC1D23", view.vehiclePlate());
    assertEquals("Lavagem Completa", view.serviceName());
    assertEquals(ServiceOrderStatus.WASHING, view.status());
    assertEquals("Lava Teste", view.establishmentName());
    assertThrows(TicketNotFoundError.class, () -> ticket.view("x".repeat(32)));
    assertThrows(TicketNotFoundError.class, () -> ticket.view("../etc"));
  }

  @Test
  void whatsappCodeSignsUpNewCustomersAndSignsInExistingOnes() throws Exception {
    Jdbi jdbi = createJdbi();
    CapturingSender sender = new CapturingSender();
    DbCustomerPasswordlessLogin login = login(jdbi, sender, new FakeGoogle(null));

    login.requestCode(OtpRecipient.phone("(92) 99123-4567"));
    assertTrue(sender.link.endsWith("/entrar/whatsapp?telefone=92991234567&codigo=" + sender.code));
    assertThrows(OtpTooSoonError.class, () -> login.requestCode(PHONE));
    assertThrows(InvalidOtpError.class, () -> login.verifyCode(PHONE, "000000x", null));

    // Right code, new phone, no name yet: the code stays usable for the retry.
    assertThrows(IllegalArgumentException.class, () -> login.verifyCode(PHONE, sender.code, null));
    User created = login.verifyCode(PHONE, sender.code, "Joana");
    assertEquals(UserProfile.CUSTOMER, created.profile());
    assertEquals("92991234567", created.phone());
    assertThrows(
        InvalidOtpError.class,
        () -> login.verifyCode(PHONE, sender.code, "Joana"),
        "a code works once");

    // Staff phones never sign in through the customer code.
    DbCustomerPasswordlessLogin later = login(jdbi, sender, new FakeGoogle(null));
    OtpRecipient washer = OtpRecipient.phone(DEMO_WASHER_PHONE);
    later.requestCode(washer);
    assertThrows(UserProfileMismatchError.class, () -> later.verifyCode(washer, sender.code, null));
  }

  @Test
  void emailCodeSignsUpNewCustomersAndSignsInExistingOnes() throws Exception {
    Jdbi jdbi = createJdbi();
    CapturingSender email = new CapturingSender();
    DbCustomerPasswordlessLogin login = login(jdbi, new CapturingSender(), email, null);
    OtpRecipient ana = OtpRecipient.email(" Ana@Email.com ");

    login.requestCode(ana);
    assertEquals("ana@email.com", email.address);
    assertTrue(email.link.endsWith("/entrar/email?email=ana%40email.com&codigo=" + email.code));
    assertThrows(OtpTooSoonError.class, () -> login.requestCode(ana));

    assertThrows(IllegalArgumentException.class, () -> login.verifyCode(ana, email.code, null));
    User created = login.verifyCode(ana, email.code, "Ana");
    assertEquals(UserProfile.CUSTOMER, created.profile());
    assertEquals("ana@email.com", created.email());
    assertNull(created.phone());

    // A later code signs in to the same account, without asking the name again.
    DbCustomerPasswordlessLogin later = login(jdbi, new CapturingSender(), email, null);
    later.requestCode(ana);
    assertEquals(created.id(), later.verifyCode(ana, email.code, null).id());
  }

  @Test
  void anEmailCodeIsNotAPhoneCode() throws Exception {
    Jdbi jdbi = createJdbi();
    CapturingSender whatsapp = new CapturingSender();
    CapturingSender email = new CapturingSender();
    DbCustomerPasswordlessLogin login = login(jdbi, whatsapp, email, null);

    login.requestCode(OtpRecipient.email("ana@email.com"));
    login.requestCode(PHONE);

    assertThrows(InvalidOtpError.class, () -> login.verifyCode(PHONE, email.code, "Ana"));
    assertEquals("92991234567", login.verifyCode(PHONE, whatsapp.code, "Ana").phone());
  }

  @Test
  void aFailedEmailCanBeAskedAgainRightAway() throws Exception {
    Jdbi jdbi = createJdbi();
    CapturingSender email = new CapturingSender();
    email.failNext = true;
    DbCustomerPasswordlessLogin login = login(jdbi, new CapturingSender(), email, null);
    OtpRecipient ana = OtpRecipient.email("ana@email.com");

    assertThrows(OtpUnavailableError.class, () -> login.requestCode(ana));
    assertEquals(600, login.requestCode(ana), "no 60 s wait after a delivery failure");
  }

  @Test
  void emailIsRefusedWhenItsChannelIsOff() {
    DbCustomerPasswordlessLogin login =
        login(createJdbi(), new CapturingSender(), new UnavailableOtpSender("off"), null);

    assertFalse(login.options().emailCode());
    OtpUnavailableError error =
        assertThrows(
            OtpUnavailableError.class,
            () -> login.requestCode(OtpRecipient.email("ana@email.com")));
    assertEquals("Envio de código por e-mail não está configurado", error.getMessage());
  }

  @Test
  void wrongCodesBurnTheCodeAfterFiveAttempts() throws Exception {
    Jdbi jdbi = createJdbi();
    CapturingSender sender = new CapturingSender();
    DbCustomerPasswordlessLogin login = login(jdbi, sender, new FakeGoogle(null));
    login.requestCode(PHONE);
    String wrong = sender.code.equals("000000") ? "111111" : "000000";

    for (int i = 0; i < 5; i++) {
      assertThrows(InvalidOtpError.class, () -> login.verifyCode(PHONE, wrong, "Ana"));
    }
    assertThrows(InvalidOtpError.class, () -> login.verifyCode(PHONE, sender.code, "Ana"));
  }

  @Test
  void googleSignInCreatesThenReusesTheLinkedCustomer() throws Exception {
    Jdbi jdbi = createJdbi();
    FakeGoogle google = new FakeGoogle("client-id");
    DbCustomerPasswordlessLogin login = login(jdbi, new CapturingSender(), google);

    assertEquals("client-id", login.options().googleClientId());
    User first = login.signInWithGoogle("token-1");
    User again = login.signInWithGoogle("token-1");

    assertEquals(first.id(), again.id());
    assertEquals("ana@gmail.com", first.email());
    assertNull(first.phone());
    assertThrows(InvalidCredentialsError.class, () -> login.signInWithGoogle("bad"));
  }

  @Test
  void googleIsReportedAsDisabledWithoutClientId() {
    DbCustomerPasswordlessLogin login =
        login(createJdbi(), new CapturingSender(), new FakeGoogle(null));

    assertNull(login.options().googleClientId());
    assertTrue(login.options().whatsappCode());
    assertTrue(login.options().emailCode());
  }

  private static DbCustomerPasswordlessLogin login(
      Jdbi jdbi, OtpSender sender, GoogleTokenVerifier google) {
    return login(jdbi, sender, new CapturingSender(), google);
  }

  private static DbCustomerPasswordlessLogin login(
      Jdbi jdbi, OtpSender whatsapp, OtpSender email, GoogleTokenVerifier google) {
    UserJdbiRepository users = new UserJdbiRepository(jdbi);
    AccessJdbiRepository access = new AccessJdbiRepository(jdbi);
    return new DbCustomerPasswordlessLogin(
        access,
        whatsapp,
        email,
        HASHER,
        users,
        users,
        access,
        google == null ? new FakeGoogle(null) : google,
        "https://app.test/",
        Clock.fixed(Instant.now(), ZoneOffset.UTC));
  }

  private static final class CapturingSender implements OtpSender {
    String address;
    String code;
    String link;
    boolean failNext;

    @Override
    public void send(String address, String code, String magicLink) throws OtpUnavailableError {
      if (failNext) {
        failNext = false;
        throw new OtpUnavailableError("SMTP fora do ar");
      }
      this.address = address;
      this.code = code;
      this.link = magicLink;
    }

    @Override
    public boolean available() {
      return true;
    }
  }

  private record FakeGoogle(String clientId) implements GoogleTokenVerifier {
    @Override
    public Optional<GoogleIdentity> verify(String idToken) {
      return "token-1".equals(idToken)
          ? Optional.of(new GoogleIdentity("google-sub-1", "Ana@Gmail.com", "Ana Souza"))
          : Optional.empty();
    }
  }

  private static Jdbi createJdbi() {
    DataSource dataSource = TestDatabase.newDatabase();
    return TestDatabase.prepare(dataSource);
  }
}
