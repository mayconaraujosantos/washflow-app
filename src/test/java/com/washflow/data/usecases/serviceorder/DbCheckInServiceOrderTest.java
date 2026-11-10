package com.washflow.data.usecases.serviceorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.serviceorder.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.serviceorder.UpdateServiceOrderStatusRepository;
import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.domain.errors.serviceorder.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.serviceorder.CheckInServiceOrder;
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
class DbCheckInServiceOrderTest {

  @Mock private LoadServiceOrderByIdRepository loadServiceOrderByIdRepository;
  @Mock private UpdateServiceOrderStatusRepository updateServiceOrderStatusRepository;

  private DbCheckInServiceOrder sut;
  private CheckInServiceOrder.Params params;
  private ServiceOrder scheduledServiceOrder;

  @BeforeEach
  void setUp() {
    sut =
        new DbCheckInServiceOrder(
            loadServiceOrderByIdRepository, updateServiceOrderStatusRepository);

    UUID serviceOrderId = UUID.randomUUID();
    params = new CheckInServiceOrder.Params(serviceOrderId);
    Instant now = Instant.parse("2026-09-20T10:00:00Z");
    scheduledServiceOrder =
        new ServiceOrder(
            serviceOrderId,
            UUID.randomUUID(),
            1,
            null,
            ServiceOrderStatus.SCHEDULED,
            now,
            now,
            now);
  }

  @Test
  void movesToWaitingInYardWhenServiceOrderIsScheduled() throws Exception {
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.of(scheduledServiceOrder));
    ServiceOrder waitingServiceOrder =
        new ServiceOrder(
            scheduledServiceOrder.id(),
            scheduledServiceOrder.vehicleId(),
            scheduledServiceOrder.servicePriceId(),
            null,
            ServiceOrderStatus.WAITING_IN_YARD,
            scheduledServiceOrder.scheduledAt(),
            scheduledServiceOrder.createdAt(),
            Instant.now());
    when(updateServiceOrderStatusRepository.updateStatus(
            params.serviceOrderId(),
            ServiceOrderStatus.SCHEDULED,
            ServiceOrderStatus.WAITING_IN_YARD,
            null))
        .thenReturn(Optional.of(waitingServiceOrder));

    ServiceOrder result = sut.checkIn(params);

    assertEquals(ServiceOrderStatus.WAITING_IN_YARD, result.status());
    verify(updateServiceOrderStatusRepository)
        .updateStatus(
            params.serviceOrderId(),
            ServiceOrderStatus.SCHEDULED,
            ServiceOrderStatus.WAITING_IN_YARD,
            null);
  }

  @Test
  void throwsServiceOrderNotFoundWhenServiceOrderDoesNotExist() {
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.empty());

    assertThrows(ServiceOrderNotFoundError.class, () -> sut.checkIn(params));
    verifyNoInteractions(updateServiceOrderStatusRepository);
  }

  @Test
  void throwsInvalidServiceOrderStatusWhenNotScheduled() {
    ServiceOrder alreadyWaiting =
        new ServiceOrder(
            scheduledServiceOrder.id(),
            scheduledServiceOrder.vehicleId(),
            scheduledServiceOrder.servicePriceId(),
            null,
            ServiceOrderStatus.WAITING_IN_YARD,
            scheduledServiceOrder.scheduledAt(),
            scheduledServiceOrder.createdAt(),
            scheduledServiceOrder.updatedAt());
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.of(alreadyWaiting));

    assertThrows(InvalidServiceOrderStatusError.class, () -> sut.checkIn(params));
    verifyNoInteractions(updateServiceOrderStatusRepository);
  }
}
