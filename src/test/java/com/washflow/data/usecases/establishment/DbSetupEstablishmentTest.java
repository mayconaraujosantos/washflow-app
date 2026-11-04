package com.washflow.data.usecases.establishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.db.PlatformRepository;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.EstablishmentAlreadyConfiguredError;
import com.washflow.domain.errors.InvalidSetupCodeError;
import com.washflow.domain.usecases.SetupEstablishment;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DbSetupEstablishmentTest {

  private static final Instant NOW = Instant.parse("2026-11-03T14:00:00Z");

  @Mock private PlatformRepository platform;
  @Mock private PasswordHasher passwordHasher;

  @BeforeEach
  void setUp() {
    when(platform.adminExists()).thenReturn(false);
    when(platform.createFirstAdmin(any(), anyString())).thenReturn(true);
    when(passwordHasher.hash(anyString())).thenReturn("bcrypt");
  }

  private DbSetupEstablishment sut(String code, boolean required) {
    return new DbSetupEstablishment(
        platform, passwordHasher, code, required, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createsThePlatformAdministrator() throws Exception {
    User admin = sut("", false).setup(params(null));

    verify(platform).createFirstAdmin(any(), eq("bcrypt"));
    assertEquals(UserProfile.ADMIN, admin.profile());
    assertEquals("dono@washflow.com", admin.email());
    assertNull(admin.phone());
    assertEquals(NOW, admin.createdAt());
  }

  @Test
  void reportsWhetherThePlatformIsConfigured() {
    assertFalse(sut("", false).status().configured());
    when(platform.adminExists()).thenReturn(true);
    assertTrue(sut("x", true).status().configured());
    assertTrue(sut("x", true).status().setupCodeRequired());
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

    verify(platform).createFirstAdmin(any(), anyString());
  }

  @Test
  void runsOnlyOnce() {
    when(platform.adminExists()).thenReturn(true);

    assertThrows(
        EstablishmentAlreadyConfiguredError.class, () -> sut("", false).setup(params(null)));
    verify(platform, never()).createFirstAdmin(any(), anyString());
  }

  @Test
  void losesTheRaceToAnotherWizard() {
    when(platform.createFirstAdmin(any(), anyString())).thenReturn(false);

    assertThrows(
        EstablishmentAlreadyConfiguredError.class, () -> sut("", false).setup(params(null)));
  }

  @Test
  void validatesNameEmailAndPassword() {
    DbSetupEstablishment sut = sut("", false);
    assertThrows(
        IllegalArgumentException.class,
        () -> sut.setup(new SetupEstablishment.Params(null, " ", "a@b.com", "senha-forte")));
    assertThrows(
        IllegalArgumentException.class,
        () -> sut.setup(new SetupEstablishment.Params(null, "Ana", "sem-arroba", "senha-forte")));
    assertThrows(
        IllegalArgumentException.class,
        () -> sut.setup(new SetupEstablishment.Params(null, "Ana", "a@b.com", "curta")));
  }

  private static SetupEstablishment.Params params(String code) {
    return new SetupEstablishment.Params(
        code, "Dono da Plataforma", "Dono@Washflow.com", "senha-forte");
  }
}
