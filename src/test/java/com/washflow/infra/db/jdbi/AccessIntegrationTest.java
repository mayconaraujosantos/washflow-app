package com.washflow.infra.db.jdbi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.washflow.data.protocols.identity.GoogleTokenVerifier;
import com.washflow.data.protocols.messaging.OtpSender;
import com.washflow.data.usecases.access.DbCustomerPasswordlessLogin;
import com.washflow.data.usecases.access.DbServiceOrderTicket;
import com.washflow.data.usecases.access.DbWasherBadge;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.entities.TicketView;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.AccountLockedError;
import com.washflow.domain.errors.InvalidCredentialsError;
import com.washflow.domain.errors.InvalidOtpError;
import com.washflow.domain.errors.OtpTooSoonError;
import com.washflow.domain.errors.TicketNotFoundError;
import com.washflow.domain.errors.UserProfileMismatchError;
import com.washflow.domain.usecases.WasherBadge;
import com.washflow.infra.security.BCryptPasswordHasher;
import com.washflow.infra.security.HmacTokenHasher;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;

class AccessIntegrationTest {

  private static final UUID DEMO_WASHER = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final String DEMO_WASHER_PHONE = "11999991111";
  private static final HmacTokenHasher HASHER = new HmacTokenHasher("test-secret-0123456789abcdef");

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

    login.requestCode("(92) 99123-4567");
    assertTrue(sender.link.endsWith("/entrar/whatsapp?telefone=92991234567&codigo=" + sender.code));
    assertThrows(OtpTooSoonError.class, () -> login.requestCode("92991234567"));
    assertThrows(InvalidOtpError.class, () -> login.verifyCode("92991234567", "000000x", null));

    // Right code, new phone, no name yet: the code stays usable for the retry.
    assertThrows(
        IllegalArgumentException.class, () -> login.verifyCode("92991234567", sender.code, null));
    User created = login.verifyCode("92991234567", sender.code, "Joana");
    assertEquals(UserProfile.CUSTOMER, created.profile());
    assertEquals("92991234567", created.phone());
    assertThrows(
        InvalidOtpError.class,
        () -> login.verifyCode("92991234567", sender.code, "Joana"),
        "a code works once");

    // Staff phones never sign in through the customer code.
    DbCustomerPasswordlessLogin later = login(jdbi, sender, new FakeGoogle(null));
    later.requestCode(DEMO_WASHER_PHONE);
    assertThrows(
        UserProfileMismatchError.class,
        () -> later.verifyCode(DEMO_WASHER_PHONE, sender.code, null));
  }

  @Test
  void wrongCodesBurnTheCodeAfterFiveAttempts() throws Exception {
    Jdbi jdbi = createJdbi();
    CapturingSender sender = new CapturingSender();
    DbCustomerPasswordlessLogin login = login(jdbi, sender, new FakeGoogle(null));
    login.requestCode("92991234567");
    String wrong = sender.code.equals("000000") ? "111111" : "000000";

    for (int i = 0; i < 5; i++) {
      assertThrows(InvalidOtpError.class, () -> login.verifyCode("92991234567", wrong, "Ana"));
    }
    assertThrows(InvalidOtpError.class, () -> login.verifyCode("92991234567", sender.code, "Ana"));
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
  }

  private static DbCustomerPasswordlessLogin login(
      Jdbi jdbi, OtpSender sender, GoogleTokenVerifier google) {
    UserJdbiRepository users = new UserJdbiRepository(jdbi);
    AccessJdbiRepository access = new AccessJdbiRepository(jdbi);
    return new DbCustomerPasswordlessLogin(
        access,
        sender,
        HASHER,
        users,
        users,
        users,
        access,
        google,
        "https://app.test/",
        Clock.fixed(Instant.now(), ZoneOffset.UTC));
  }

  private static final class CapturingSender implements OtpSender {
    String code;
    String link;

    @Override
    public void send(String phone, String code, String magicLink) {
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
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL(
        "jdbc:h2:mem:"
            + UUID.randomUUID().toString().replace("-", "")
            + ";MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration-h2")
        .load()
        .migrate();
    return TestDatabase.prepare(dataSource);
  }
}
