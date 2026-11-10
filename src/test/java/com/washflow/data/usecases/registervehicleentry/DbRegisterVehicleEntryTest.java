package com.washflow.data.usecases.registervehicleentry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.CountServiceOrdersScheduledInRangeRepository;
import com.washflow.data.protocols.db.CreateServiceOrderRepository;
import com.washflow.data.protocols.db.CreateVehicleRepository;
import com.washflow.data.protocols.db.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.LoadServicePriceByIdRepository;
import com.washflow.data.protocols.db.LoadUserByPhoneRepository;
import com.washflow.data.protocols.db.LoadVehicleByPlateRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.entities.ServicePrice;
import com.washflow.domain.entities.User;
import com.washflow.domain.entities.UserProfile;
import com.washflow.domain.entities.Vehicle;
import com.washflow.domain.errors.cash.ServicePriceNotFoundError;
import com.washflow.domain.errors.schedule.OutsideBusinessHoursError;
import com.washflow.domain.errors.schedule.SlotFullyBookedError;
import com.washflow.domain.errors.vehicle.InvalidPlateError;
import com.washflow.domain.usecases.RegisterVehicleEntry;
import java.math.BigDecimal;
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

@ExtendWith(MockitoExtension.class)
class DbRegisterVehicleEntryTest {

  // Tuesday 10:00 in Manaus (UTC-4).
  private static final Instant NOW = Instant.parse("2026-11-03T14:00:00Z");

  @Mock private LoadVehicleByPlateRepository loadVehicleByPlateRepository;
  @Mock private CreateVehicleRepository createVehicleRepository;
  @Mock private LoadUserByPhoneRepository loadUserByPhoneRepository;
  @Mock private LoadServicePriceByIdRepository loadServicePriceByIdRepository;
  @Mock private CountServiceOrdersScheduledInRangeRepository countRepository;
  @Mock private CreateServiceOrderRepository createServiceOrderRepository;
  @Mock private LoadServiceOrderByIdRepository loadServiceOrderByIdRepository;

  private DbRegisterVehicleEntry sut;

  @BeforeEach
  void setUp() {
    sut =
        new DbRegisterVehicleEntry(
            loadVehicleByPlateRepository,
            createVehicleRepository,
            loadUserByPhoneRepository,
            loadServicePriceByIdRepository,
            countRepository,
            createServiceOrderRepository,
            loadServiceOrderByIdRepository,
            Clock.fixed(NOW, ZoneOffset.UTC),
            2);
  }

  @Test
  void walkInWithNewPlateCreatesVehicleWithContactAndGoesStraightToTheYard() throws Exception {
    givenServiceExists();
    when(loadVehicleByPlateRepository.loadByPlate("ABC1D23")).thenReturn(Optional.empty());
    when(loadUserByPhoneRepository.loadByPhone("11987654321")).thenReturn(Optional.empty());
    when(createVehicleRepository.create(any())).thenAnswer(call -> call.getArgument(0));
    givenOrderIsPersisted();

    ServiceOrder order =
        sut.register(params("abc-1d23", "+55 (11) 98765-4321", null, NOW.plusSeconds(7200)));

    ArgumentCaptor<Vehicle> vehicle = ArgumentCaptor.forClass(Vehicle.class);
    verify(createVehicleRepository).create(vehicle.capture());
    assertEquals("ABC1D23", vehicle.getValue().plate());
    assertNull(vehicle.getValue().customerId());
    assertEquals("Maria", vehicle.getValue().contactName());
    assertEquals("11987654321", vehicle.getValue().contactPhone());
    assertEquals(ServiceOrderStatus.WAITING_IN_YARD, order.status());
    assertEquals(NOW, order.scheduledAt());
    assertEquals(NOW.plusSeconds(7200), order.expectedDeliveryAt());
    assertEquals("Riscado na porta", order.notes());
    verifyNoInteractions(countRepository);
  }

  @Test
  void linksNewVehicleToTheCustomerAccountThatOwnsThePhone() throws Exception {
    UUID customerId = UUID.randomUUID();
    givenServiceExists();
    when(loadVehicleByPlateRepository.loadByPlate("ABC1234")).thenReturn(Optional.empty());
    when(loadUserByPhoneRepository.loadByPhone("11987654321"))
        .thenReturn(
            Optional.of(new User(customerId, "Maria", "11987654321", UserProfile.CUSTOMER, NOW)));
    when(createVehicleRepository.create(any())).thenAnswer(call -> call.getArgument(0));
    givenOrderIsPersisted();

    sut.register(params("ABC1234", "11987654321", null, null));

    ArgumentCaptor<Vehicle> vehicle = ArgumentCaptor.forClass(Vehicle.class);
    verify(createVehicleRepository).create(vehicle.capture());
    assertEquals(customerId, vehicle.getValue().customerId());
    assertNull(vehicle.getValue().contactName());
  }

  @Test
  void reusesTheVehicleWhenThePlateIsAlreadyKnown() throws Exception {
    UUID vehicleId = UUID.randomUUID();
    givenServiceExists();
    when(loadVehicleByPlateRepository.loadByPlate("ABC1D23"))
        .thenReturn(Optional.of(new Vehicle(vehicleId, null, "ABC1D23", "Onix", "Prata")));
    givenOrderIsPersisted();

    ServiceOrder order = sut.register(params("ABC1D23", null, null, null));

    assertEquals(vehicleId, order.vehicleId());
    verify(createVehicleRepository, never()).create(any());
  }

  @Test
  void schedulingForLaterChecksBusinessHoursAndCapacity() throws Exception {
    Instant tomorrowAtNine = Instant.parse("2026-11-04T13:00:00Z");
    givenServiceExists();
    when(countRepository.countScheduledBetween(any(), any())).thenReturn(0);
    when(loadVehicleByPlateRepository.loadByPlate("ABC1D23"))
        .thenReturn(Optional.of(new Vehicle(UUID.randomUUID(), null, "ABC1D23", null, null)));
    givenOrderIsPersisted();

    ServiceOrder order = sut.register(params("ABC1D23", null, tomorrowAtNine, null));

    assertEquals(ServiceOrderStatus.SCHEDULED, order.status());
    assertEquals(tomorrowAtNine, order.scheduledAt());
  }

  @Test
  void rejectsSchedulingOutsideBusinessHours() {
    givenServiceExists();
    Instant sundayMorning = Instant.parse("2026-11-08T13:00:00Z");

    assertThrows(
        OutsideBusinessHoursError.class,
        () -> sut.register(params("ABC1D23", null, sundayMorning, null)));
    verifyNoInteractions(createServiceOrderRepository);
  }

  @Test
  void rejectsSchedulingAFullSlot() {
    givenServiceExists();
    when(countRepository.countScheduledBetween(any(), any())).thenReturn(2);

    assertThrows(
        SlotFullyBookedError.class,
        () -> sut.register(params("ABC1D23", null, Instant.parse("2026-11-04T13:00:00Z"), null)));
  }

  @Test
  void rejectsInvalidPlateBeforeTouchingTheDatabase() {
    assertThrows(InvalidPlateError.class, () -> sut.register(params("AB-12", null, null, null)));
    verifyNoInteractions(loadServicePriceByIdRepository, createServiceOrderRepository);
  }

  @Test
  void rejectsInvalidPhone() {
    assertThrows(
        IllegalArgumentException.class, () -> sut.register(params("ABC1D23", "1234", null, null)));
  }

  @Test
  void rejectsExpectedDeliveryBeforeTheEntry() {
    assertThrows(
        IllegalArgumentException.class,
        () -> sut.register(params("ABC1D23", null, null, NOW.minusSeconds(60))));
  }

  @Test
  void rejectsUnknownService() {
    when(loadServicePriceByIdRepository.loadById(1)).thenReturn(Optional.empty());

    assertThrows(
        ServicePriceNotFoundError.class, () -> sut.register(params("ABC1D23", null, null, null)));
  }

  private void givenServiceExists() {
    when(loadServicePriceByIdRepository.loadById(1))
        .thenReturn(
            Optional.of(
                new ServicePrice(1, "Lavagem Completa", new BigDecimal("60.00"), BigDecimal.TEN)));
  }

  private void givenOrderIsPersisted() {
    // Walk-ins use create, bookings createWithinCapacity (atomic per-hour capacity).
    lenient()
        .when(createServiceOrderRepository.create(any()))
        .thenAnswer(call -> call.getArgument(0));
    lenient()
        .when(createServiceOrderRepository.createWithinCapacity(any(), any(), any(), anyInt()))
        .thenAnswer(call -> Optional.of(call.getArgument(0)));
    when(loadServiceOrderByIdRepository.loadById(any())).thenReturn(Optional.empty());
  }

  private static RegisterVehicleEntry.Params params(
      String plate, String phone, Instant scheduledAt, Instant expectedDeliveryAt) {
    return new RegisterVehicleEntry.Params(
        plate,
        "Onix",
        "Prata",
        "Maria",
        phone,
        1,
        scheduledAt,
        expectedDeliveryAt,
        "Riscado na porta");
  }
}
