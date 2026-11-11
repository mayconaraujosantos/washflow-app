package com.washflow.data.usecases.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.user.LoadUserByIdRepository;
import com.washflow.data.protocols.db.vehicle.ClaimVehicleRepository;
import com.washflow.data.protocols.db.vehicle.CreateVehicleRepository;
import com.washflow.data.protocols.db.vehicle.LoadVehicleByPlateRepository;
import com.washflow.domain.entities.user.User;
import com.washflow.domain.entities.user.UserProfile;
import com.washflow.domain.entities.vehicle.Vehicle;
import com.washflow.domain.errors.user.UserNotFoundError;
import com.washflow.domain.errors.vehicle.InvalidPlateError;
import com.washflow.domain.errors.vehicle.VehiclePlateAlreadyRegisteredError;
import com.washflow.domain.usecases.vehicle.CreateVehicle;
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

  private static final String PHONE = "11999990000";

  @Mock private LoadUserByIdRepository loadUserByIdRepository;
  @Mock private LoadVehicleByPlateRepository loadVehicleByPlateRepository;
  @Mock private CreateVehicleRepository createVehicleRepository;
  @Mock private ClaimVehicleRepository claimVehicleRepository;

  private DbCreateVehicle sut;
  private CreateVehicle.Params params;
  private User customer;

  @BeforeEach
  void setUp() {
    sut =
        new DbCreateVehicle(
            loadUserByIdRepository,
            loadVehicleByPlateRepository,
            createVehicleRepository,
            claimVehicleRepository);

    UUID customerId = UUID.randomUUID();
    params = new CreateVehicle.Params(customerId, "ABC1D23", "Onix", "Prata");
    customer = new User(customerId, "Cliente Demo", PHONE, UserProfile.CUSTOMER, Instant.now());
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
  void normalizesThePlateSoItMatchesTheCounterEntry() throws Exception {
    when(loadUserByIdRepository.loadById(params.customerId())).thenReturn(Optional.of(customer));
    when(loadVehicleByPlateRepository.loadByPlate("ABC1234")).thenReturn(Optional.empty());
    when(createVehicleRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Vehicle vehicle =
        sut.create(new CreateVehicle.Params(params.customerId(), " abc-1234 ", " ", null));

    assertEquals("ABC1234", vehicle.plate());
    assertEquals(null, vehicle.model());
  }

  @Test
  void rejectsAnInvalidPlate() {
    when(loadUserByIdRepository.loadById(params.customerId())).thenReturn(Optional.of(customer));

    assertThrows(
        InvalidPlateError.class,
        () -> sut.create(new CreateVehicle.Params(params.customerId(), "AB-12", null, null)));
    verifyNoInteractions(loadVehicleByPlateRepository, createVehicleRepository);
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
    verifyNoInteractions(claimVehicleRepository);
  }

  @Test
  void takesOverTheCounterCarWhoseContactIsTheCustomersPhone() throws Exception {
    Vehicle counterCar =
        new Vehicle(UUID.randomUUID(), null, params.plate(), "Onix", "Prata", "Joao", PHONE);
    when(loadUserByIdRepository.loadById(params.customerId())).thenReturn(Optional.of(customer));
    when(loadVehicleByPlateRepository.loadByPlate(params.plate()))
        .thenReturn(Optional.of(counterCar));
    when(claimVehicleRepository.claim(counterCar.id(), customer.id(), null, null)).thenReturn(true);

    Vehicle vehicle =
        sut.create(new CreateVehicle.Params(params.customerId(), params.plate(), null, null));

    assertEquals(counterCar.id(), vehicle.id());
    assertEquals(customer.id(), vehicle.customerId());
    assertEquals("Onix", vehicle.model());
    assertEquals("Prata", vehicle.color());
    assertEquals(null, vehicle.contactPhone());
    verify(createVehicleRepository, never()).create(any());
  }

  @Test
  void doesNotHandOverACounterCarWithSomeoneElsesPhone() {
    Vehicle counterCar =
        new Vehicle(UUID.randomUUID(), null, params.plate(), "Onix", "Prata", "Ana", "92988887777");
    when(loadUserByIdRepository.loadById(params.customerId())).thenReturn(Optional.of(customer));
    when(loadVehicleByPlateRepository.loadByPlate(params.plate()))
        .thenReturn(Optional.of(counterCar));

    assertThrows(VehiclePlateAlreadyRegisteredError.class, () -> sut.create(params));
    verifyNoInteractions(claimVehicleRepository);
  }

  @Test
  void reportsTheConflictWhenAnotherRequestClaimedTheCarFirst() {
    Vehicle counterCar =
        new Vehicle(UUID.randomUUID(), null, params.plate(), "Onix", "Prata", "Joao", PHONE);
    when(loadUserByIdRepository.loadById(params.customerId())).thenReturn(Optional.of(customer));
    when(loadVehicleByPlateRepository.loadByPlate(params.plate()))
        .thenReturn(Optional.of(counterCar));
    when(claimVehicleRepository.claim(any(), any(), anyString(), anyString())).thenReturn(false);

    assertThrows(VehiclePlateAlreadyRegisteredError.class, () -> sut.create(params));
  }
}
