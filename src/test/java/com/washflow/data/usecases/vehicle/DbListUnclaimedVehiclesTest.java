package com.washflow.data.usecases.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.data.protocols.db.vehicle.ClaimVehicleRepository;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.entities.vehicle.Vehicle;
import com.washflow.domain.errors.user.UserNotFoundError;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbListUnclaimedVehiclesTest {

  @Mock private LoadUserByIdRepository loadUserByIdRepository;
  @Mock private ClaimVehicleRepository claimVehicleRepository;

  private final UUID id = UUID.randomUUID();

  @Test
  void listsCounterCarsWithTheCustomersPhone() throws Exception {
    User customer = new User(id, "Ana", "92988887777", UserProfile.CUSTOMER, Instant.now());
    Vehicle car =
        new Vehicle(UUID.randomUUID(), null, "ABC1D23", "Onix", null, "Ana", "92988887777");
    when(loadUserByIdRepository.loadById(id)).thenReturn(Optional.of(customer));
    when(claimVehicleRepository.loadUnclaimedByContactPhone("92988887777"))
        .thenReturn(List.of(car));

    assertEquals(
        List.of(car),
        new DbListUnclaimedVehicles(loadUserByIdRepository, claimVehicleRepository).list(id));
  }

  @Test
  void staffAccountsHaveNone() throws Exception {
    User washer = new User(id, "Lu", "92988887777", UserProfile.WASHER, Instant.now());
    when(loadUserByIdRepository.loadById(id)).thenReturn(Optional.of(washer));

    assertEquals(
        List.of(),
        new DbListUnclaimedVehicles(loadUserByIdRepository, claimVehicleRepository).list(id));
    verifyNoInteractions(claimVehicleRepository);
  }

  @Test
  void unknownCustomerIsNotFound() {
    when(loadUserByIdRepository.loadById(id)).thenReturn(Optional.empty());

    assertThrows(
        UserNotFoundError.class,
        () -> new DbListUnclaimedVehicles(loadUserByIdRepository, claimVehicleRepository).list(id));
  }
}
