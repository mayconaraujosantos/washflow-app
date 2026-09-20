package com.washflow.data.usecases.scheduleserviceorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.CreateServiceOrderRepository;
import com.washflow.data.protocols.db.LoadServicePriceByIdRepository;
import com.washflow.data.protocols.db.LoadVehicleByIdRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.entities.ServicePrice;
import com.washflow.domain.entities.Vehicle;
import com.washflow.domain.errors.ServicePriceNotFoundError;
import com.washflow.domain.errors.VehicleNotFoundError;
import com.washflow.domain.usecases.ScheduleServiceOrder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The payoff of keeping this use case behind {@code data.protocols.db} ports: it's tested here with
 * plain Mockito doubles, no Postgres/JDBI/Testcontainers involved at all.
 */
@ExtendWith(MockitoExtension.class)
class DbScheduleServiceOrderTest {

  @Mock private LoadVehicleByIdRepository loadVehicleByIdRepository;
  @Mock private LoadServicePriceByIdRepository loadServicePriceByIdRepository;
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
            createServiceOrderRepository);

    UUID vehicleId = UUID.randomUUID();
    params = new ScheduleServiceOrder.Params(vehicleId, 1, Instant.parse("2026-09-20T10:00:00Z"));
    vehicle = new Vehicle(vehicleId, UUID.randomUUID(), "ABC1D23", "Onix", "Prata");
    servicePrice =
        new ServicePrice(1, "Lavagem Completa", new BigDecimal("60.00"), new BigDecimal("20.00"));
  }

  @Test
  void schedulesWithStatusScheduledWhenVehicleAndServicePriceExist() throws Exception {
    when(loadVehicleByIdRepository.loadById(params.vehicleId())).thenReturn(Optional.of(vehicle));
    when(loadServicePriceByIdRepository.loadById(params.servicePriceId()))
        .thenReturn(Optional.of(servicePrice));
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
}
