package com.washflow.data.usecases.establishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.db.EstablishmentRepository;
import com.washflow.data.protocols.db.LoadUserByEmailRepository;
import com.washflow.data.protocols.db.LoadUserByPhoneRepository;
import com.washflow.domain.entities.Establishment;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.EmailAlreadyRegisteredError;
import com.washflow.domain.errors.EstablishmentAlreadyConfiguredError;
import com.washflow.domain.errors.InvalidSetupCodeError;
import com.washflow.domain.usecases.SetupEstablishment;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DbSetupEstablishmentTest {

  private static final Instant NOW = Instant.parse("2026-11-03T14:00:00Z");

  @Mock private EstablishmentRepository establishments;
  @Mock private LoadUserByEmailRepository usersByEmail;
  @Mock private LoadUserByPhoneRepository usersByPhone;
  @Mock private PasswordHasher passwordHasher;

  @BeforeEach
  void setUp() throws Exception {
    when(establishments.load())
        .thenReturn(new Establishment("Meu Lava-Jato", 8, 18, false, 3, null));
    when(usersByEmail.loadByEmail(anyString())).thenReturn(Optional.empty());
    when(usersByPhone.loadByPhone(anyString())).thenReturn(Optional.empty());
    when(passwordHasher.hash(anyString())).thenReturn("bcrypt");
    when(establishments.completeSetup(any(), any(), anyString()))
        .thenAnswer(call -> call.getArgument(1));
  }

  private DbSetupEstablishment sut(String code, boolean required) {
    return new DbSetupEstablishment(
        establishments,
        usersByEmail,
        usersByPhone,
        passwordHasher,
        code,
        required,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createsTheConfiguredEstablishmentAndItsOwnerManager() throws Exception {
    User owner = sut("", false).setup(params(null));

    ArgumentCaptor<Establishment> establishment = ArgumentCaptor.forClass(Establishment.class);
    verify(establishments).completeSetup(establishment.capture(), any(), eq("bcrypt"));
    assertEquals("Lava Rápido Centro", establishment.getValue().name());
    assertEquals(7, establishment.getValue().openingHour());
    assertEquals(NOW, establishment.getValue().configuredAt());
    assertEquals(UserProfile.MANAGER, owner.profile());
    assertEquals("dono@lavajato.com", owner.email());
    assertEquals("92991234567", owner.phone());
  }

  @Test
  void productionRequiresTheServerSetupCode() {
    assertThrows(InvalidSetupCodeError.class, () -> sut("s3gredo", true).setup(params(null)));
    assertThrows(InvalidSetupCodeError.class, () -> sut("s3gredo", true).setup(params("errado")));
    // Required but not configured on the server: setup stays disabled.
    assertThrows(InvalidSetupCodeError.class, () -> sut("", true).setup(params("")));
  }

  @Test
  void acceptsTheRightSetupCode() throws Exception {
    sut("s3gredo", true).setup(params(" s3gredo "));

    verify(establishments).completeSetup(any(), any(), anyString());
  }

  @Test
  void runsOnlyOnce() throws Exception {
    when(establishments.load())
        .thenReturn(new Establishment("Lava", 8, 18, false, 3, NOW.minusSeconds(60)));

    assertThrows(
        EstablishmentAlreadyConfiguredError.class, () -> sut("", false).setup(params(null)));
    verify(establishments, never()).completeSetup(any(), any(), anyString());
  }

  @Test
  void rejectsAnE_mailThatAlreadyHasAnAccount() {
    when(usersByEmail.loadByEmail("dono@lavajato.com"))
        .thenReturn(
            Optional.of(
                new User(UUID.randomUUID(), "X", null, UserProfile.MANAGER, NOW, true, "x")));

    assertThrows(EmailAlreadyRegisteredError.class, () -> sut("", false).setup(params(null)));
  }

  @Test
  void validatesHoursCapacityEmailAndPassword() {
    DbSetupEstablishment sut = sut("", false);
    assertThrows(IllegalArgumentException.class, () -> sut.setup(withHours(18, 8)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            sut.setup(
                new SetupEstablishment.Params(
                    null, "Lava", 8, 18, false, 0, "Ana", "a@b.com", null, "senha-forte")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            sut.setup(
                new SetupEstablishment.Params(
                    null, "Lava", 8, 18, false, 3, "Ana", "sem-arroba", null, "senha-forte")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            sut.setup(
                new SetupEstablishment.Params(
                    null, "Lava", 8, 18, false, 3, "Ana", "a@b.com", null, "curta")));
  }

  @Test
  void reportsStatusForTheWizard() {
    SetupEstablishment.Status status = sut("x", true).status();

    assertEquals(false, status.configured());
    assertEquals(true, status.setupCodeRequired());
  }

  private static SetupEstablishment.Params withHours(int opening, int closing) {
    return new SetupEstablishment.Params(
        null, "Lava", opening, closing, false, 3, "Ana", "a@b.com", null, "senha-forte");
  }

  private static SetupEstablishment.Params params(String code) {
    return new SetupEstablishment.Params(
        code,
        "Lava Rápido Centro",
        7,
        19,
        true,
        4,
        "Dono",
        " Dono@LavaJato.com ",
        "(92) 99123-4567",
        "senha-forte");
  }
}
