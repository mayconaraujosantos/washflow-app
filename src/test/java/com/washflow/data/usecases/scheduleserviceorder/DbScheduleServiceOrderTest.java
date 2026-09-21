package com.washflow.data.usecases.scheduleserviceorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.CountServiceOrdersScheduledInRangeRepository;
import com.washflow.data.protocols.db.CreateServiceOrderRepository;
import com.washflow.data.protocols.db.LoadServicePriceByIdRepository;
import com.washflow.data.protocols.db.LoadVehicleByIdRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.entities.ServicePrice;
import com.washflow.domain.entities.Vehicle;
import com.washflow.domain.errors.InsufficientLeadTimeError;
import com.washflow.domain.errors.OutsideBusinessHoursError;
import com.washflow.domain.errors.ServicePriceNotFoundError;
import com.washflow.domain.errors.SlotFullyBookedError;
import com.washflow.domain.errors.VehicleNotFoundError;
import com.washflow.domain.usecases.ScheduleServiceOrder;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The payoff of keeping this use case behind {@code data.protocols.db} ports: it's tested here with
 * plain Mockito doubles, no Postgres/JDBI/Testcontainers involved at all. {@code CLOCK} pins "now"
 * so the business-hours/lead-time/capacity rules from {@code docs/feature.spec.md} are
 * deterministic regardless of when the suite actually runs.
 */
@ExtendWith(MockitoExtension.class)
class DbScheduleServiceOrderTest {

  // Monday 2026-01-05, 09:00 America/Sao_Paulo (UTC-3).
  private static final Instant NOW = Instant.parse("2026-01-05T12:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("America/Sao_Paulo"));
  private static final int MAX_VEHICLES_PER_HOUR = 3;

  @Mock private LoadVehicleByIdRepository loadVehicleByIdRepository;
  @Mock private LoadServicePriceByIdRepository loadServicePriceByIdRepository;

  @Mock
  private CountServiceOrdersScheduledInRangeRepository countServiceOrdersScheduledInRangeRepository;

  @Mock private CreateServiceOrderRepository createServiceOrderRepository;

  private DbScheduleServiceOrder sut;
  private ScheduleServiceOrder.Params params;
  private Vehicle vehicle;
  private ServicePrice servicePrice;

  @BeforeEach
  void setUp() {
    sut =
        new DbScheduleServiceOrder(
            loadVehicleByIdRepository,
            loadServicePriceByIdRepository,
            countServiceOrdersScheduledInRangeRepository,
            createServiceOrderRepository,
            CLOCK,
            MAX_VEHICLES_PER_HOUR);

    UUID vehicleId = UUID.randomUUID();
    // Monday 2026-01-05, 11:00 local - within business hours, well past the 30min lead time.
    params = new ScheduleServiceOrder.Params(vehicleId, 1, Instant.parse("2026-01-05T14:00:00Z"));
    vehicle = new Vehicle(vehicleId, UUID.randomUUID(), "ABC1D23", "Onix", "Prata");
    servicePrice =
        new ServicePrice(1, "Lavagem Completa", new BigDecimal("60.00"), new BigDecimal("20.00"));
  }

  @Test
  void schedulesWithStatusScheduledWhenValidAndSlotHasCapacity() throws Exception {
    when(loadVehicleByIdRepository.loadById(params.vehicleId())).thenReturn(Optional.of(vehicle));
    when(loadServicePriceByIdRepository.loadById(params.servicePriceId()))
        .thenReturn(Optional.of(servicePrice));
    when(countServiceOrdersScheduledInRangeRepository.countScheduledBetween(
            Instant.parse("2026-01-05T14:00:00Z"), Instant.parse("2026-01-05T15:00:00Z")))
        .thenReturn(0);
    when(createServiceOrderRepository.create(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ServiceOrder serviceOrder = sut.schedule(params);

    assertEquals(ServiceOrderStatus.SCHEDULED, serviceOrder.status());
    assertEquals(params.vehicleId(), serviceOrder.vehicleId());
    assertEquals(params.servicePriceId(), serviceOrder.servicePriceId());
    assertEquals(params.scheduledAt(), serviceOrder.scheduledAt());
    verify(createServiceOrderRepository).create(any());
  }

  @Test
  void throwsVehicleNotFoundWhenVehicleDoesNotExist() {
    when(loadVehicleByIdRepository.loadById(params.vehicleId())).thenReturn(Optional.empty());

    assertThrows(VehicleNotFoundError.class, () -> sut.schedule(params));
    verifyNoInteractions(createServiceOrderRepository);
  }

  @Test
  void throwsServicePriceNotFoundWhenServicePriceDoesNotExist() {
    when(loadVehicleByIdRepository.loadById(params.vehicleId())).thenReturn(Optional.of(vehicle));
    when(loadServicePriceByIdRepository.loadById(params.servicePriceId()))
        .thenReturn(Optional.empty());

    assertThrows(ServicePriceNotFoundError.class, () -> sut.schedule(params));
    verifyNoInteractions(createServiceOrderRepository);
  }

  @Test
  void throwsOutsideBusinessHoursWhenScheduledOnSunday() {
    // Sunday 2026-01-11, 10:00 local.
    var sundayParams =
        new ScheduleServiceOrder.Params(
            params.vehicleId(), params.servicePriceId(), Instant.parse("2026-01-11T13:00:00Z"));

    assertThrows(OutsideBusinessHoursError.class, () -> sut.schedule(sundayParams));
    verifyNoInteractions(loadVehicleByIdRepository, createServiceOrderRepository);
  }

  @Test
  void throwsOutsideBusinessHoursWhenScheduledAfterClosing() {
    // Monday 2026-01-05, 19:00 local - after the 18:00 close.
    var afterHoursParams =
        new ScheduleServiceOrder.Params(
            params.vehicleId(), params.servicePriceId(), Instant.parse("2026-01-05T22:00:00Z"));

    assertThrows(OutsideBusinessHoursError.class, () -> sut.schedule(afterHoursParams));
    verifyNoInteractions(loadVehicleByIdRepository, createServiceOrderRepository);
  }

  @Test
  void throwsInsufficientLeadTimeWhenLessThan30MinutesAhead() {
    // 10 minutes after NOW - still within business hours (09:10 local), just too soon.
    var tooSoonParams =
        new ScheduleServiceOrder.Params(
            params.vehicleId(), params.servicePriceId(), Instant.parse("2026-01-05T12:10:00Z"));

    assertThrows(InsufficientLeadTimeError.class, () -> sut.schedule(tooSoonParams));
    verifyNoInteractions(loadVehicleByIdRepository, createServiceOrderRepository);
  }

  @Test
  void throwsSlotFullyBookedWhenHourAlreadyHasMaxVehicles() {
    when(loadVehicleByIdRepository.loadById(params.vehicleId())).thenReturn(Optional.of(vehicle));
    when(loadServicePriceByIdRepository.loadById(params.servicePriceId()))
        .thenReturn(Optional.of(servicePrice));
    when(countServiceOrdersScheduledInRangeRepository.countScheduledBetween(any(), any()))
        .thenReturn(MAX_VEHICLES_PER_HOUR);

    assertThrows(SlotFullyBookedError.class, () -> sut.schedule(params));
    verifyNoInteractions(createServiceOrderRepository);
  }
}
