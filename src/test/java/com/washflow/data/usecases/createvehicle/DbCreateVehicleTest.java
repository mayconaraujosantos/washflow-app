package com.washflow.data.usecases.createvehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.CreateVehicleRepository;
import com.washflow.data.protocols.db.LoadUserByIdRepository;
import com.washflow.data.protocols.db.LoadVehicleByPlateRepository;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.entities.Vehicle;
import com.washflow.domain.errors.UserNotFoundError;
import com.washflow.domain.errors.VehiclePlateAlreadyRegisteredError;
import com.washflow.domain.usecases.CreateVehicle;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Same payoff as {@code DbScheduleServiceOrderTest}: exercised here with plain Mockito doubles, no
 * Postgres/JDBI/Testcontainers involved at all.
 */
@ExtendWith(MockitoExtension.class)
class DbCreateVehicleTest {

  @Mock private LoadUserByIdRepository loadUserByIdRepository;
  @Mock private LoadVehicleByPlateRepository loadVehicleByPlateRepository;
  @Mock private CreateVehicleRepository createVehicleRepository;

  private DbCreateVehicle sut;
  private CreateVehicle.Params params;
  private User customer;

  @BeforeEach
  void setUp() {
    sut =
        new DbCreateVehicle(
            loadUserByIdRepository, loadVehicleByPlateRepository, createVehicleRepository);

    UUID customerId = UUID.randomUUID();
    params = new CreateVehicle.Params(customerId, "ABC1D23", "Onix", "Prata");
    customer =
        new User(customerId, "Cliente Demo", "11999990000", UserProfile.CUSTOMER, Instant.now());
  }

  @Test
  void createsVehicleWhenCustomerExistsAndPlateIsFree() throws Exception {
    when(loadUserByIdRepository.loadById(params.customerId())).thenReturn(Optional.of(customer));
    when(loadVehicleByPlateRepository.loadByPlate(params.plate())).thenReturn(Optional.empty());
    when(createVehicleRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Vehicle vehicle = sut.create(params);

    assertEquals(params.customerId(), vehicle.customerId());
    assertEquals(params.plate(), vehicle.plate());
    assertEquals(params.model(), vehicle.model());
    assertEquals(params.color(), vehicle.color());
    verify(createVehicleRepository).create(any());
  }

  @Test
  void throwsUserNotFoundWhenCustomerDoesNotExist() {
    when(loadUserByIdRepository.loadById(params.customerId())).thenReturn(Optional.empty());

    assertThrows(UserNotFoundError.class, () -> sut.create(params));
    verifyNoInteractions(loadVehicleByPlateRepository, createVehicleRepository);
  }

  @Test
  void throwsVehiclePlateAlreadyRegisteredWhenPlateIsTaken() {
    Vehicle existingVehicle =
        new Vehicle(UUID.randomUUID(), UUID.randomUUID(), params.plate(), "Civic", "Preto");
    when(loadUserByIdRepository.loadById(params.customerId())).thenReturn(Optional.of(customer));
    when(loadVehicleByPlateRepository.loadByPlate(params.plate()))
        .thenReturn(Optional.of(existingVehicle));

    assertThrows(VehiclePlateAlreadyRegisteredError.class, () -> sut.create(params));
    verify(createVehicleRepository, never()).create(any());
  }
}
