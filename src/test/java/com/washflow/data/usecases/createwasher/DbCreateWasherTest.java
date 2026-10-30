package com.washflow.data.usecases.createwasher;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.PasswordHasher;
import com.washflow.data.protocols.db.CreateWasherRepository;
import com.washflow.data.protocols.db.LoadUserByPhoneRepository;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.errors.PhoneAlreadyRegisteredError;
import com.washflow.domain.usecases.CreateWasher;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbCreateWasherTest {

  @Mock private CreateWasherRepository createRepository;
  @Mock private LoadUserByPhoneRepository loadByPhoneRepository;
  @Mock private PasswordHasher passwordHasher;

  private DbCreateWasher sut;

  @BeforeEach
  void setUp() {
    sut = new DbCreateWasher(createRepository, loadByPhoneRepository, passwordHasher);
  }

  @Test
  void createsWasherSuccessfully() throws Exception {
    when(loadByPhoneRepository.loadByPhone("11999999999")).thenReturn(Optional.empty());
    when(passwordHasher.hash("password123")).thenReturn("$2a$hashed");
    when(createRepository.createWasher(any(), eq("$2a$hashed")))
        .thenAnswer(inv -> inv.getArgument(0));

    CreateWasher.Params params =
        new CreateWasher.Params("Joao Silva", "11999999999", "password123");

    User result = sut.create(params);

    assertDoesNotThrow(() -> result);
    assertEquals("Joao Silva", result.name());
    assertEquals(UserProfile.WASHER, result.profile());
    assertEquals(true, result.active());
  }

  @Test
  void throwsWhenNameIsBlank() {
    CreateWasher.Params params = new CreateWasher.Params("  ", "11999999999", "password123");

    assertThrows(IllegalArgumentException.class, () -> sut.create(params));
    verifyNoInteractions(loadByPhoneRepository);
  }

  @Test
  void throwsWhenPhoneIsBlank() {
    CreateWasher.Params params = new CreateWasher.Params("Joao Silva", "", "password123");

    assertThrows(IllegalArgumentException.class, () -> sut.create(params));
    verifyNoInteractions(loadByPhoneRepository);
  }

  @Test
  void throwsWhenPasswordIsTooShort() {
    CreateWasher.Params params = new CreateWasher.Params("Joao Silva", "11999999999", "short");

    assertThrows(IllegalArgumentException.class, () -> sut.create(params));
    verifyNoInteractions(loadByPhoneRepository);
  }

  @Test
  void throwsPhoneAlreadyRegisteredWhenPhoneExists() {
    when(loadByPhoneRepository.loadByPhone("11999999999")).thenReturn(Optional.of(mockUser()));

    CreateWasher.Params params =
        new CreateWasher.Params("Joao Silva", "11999999999", "password123");

    assertThrows(PhoneAlreadyRegisteredError.class, () -> sut.create(params));
    verifyNoInteractions(createRepository);
  }

  @Test
  void trimsNameAndPhone() throws Exception {
    when(loadByPhoneRepository.loadByPhone("11999999999")).thenReturn(Optional.empty());
    when(passwordHasher.hash("password123")).thenReturn("$2a$hashed");
    when(createRepository.createWasher(any(), eq("$2a$hashed")))
        .thenAnswer(inv -> inv.getArgument(0));

    CreateWasher.Params params =
        new CreateWasher.Params("  Joao Silva  ", "  11999999999  ", "password123");

    sut.create(params);

    verify(loadByPhoneRepository).loadByPhone("11999999999");
  }

  private User mockUser() {
    return new User(
        java.util.UUID.randomUUID(),
        "Existing",
        "11999999999",
        UserProfile.CUSTOMER,
        java.time.Instant.now(),
        true);
  }
}
