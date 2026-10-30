package com.washflow.data.usecases.startserviceorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.LoadServiceOrderByIdRepository;
import com.washflow.data.protocols.db.UpdateServiceOrderStatusRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.errors.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.StartServiceOrder;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Same payoff as {@code DbCheckInServiceOrderTest}: exercised here with plain Mockito doubles, no
 * Postgres/JDBI/Testcontainers involved at all.
 */
@ExtendWith(MockitoExtension.class)
class DbStartServiceOrderTest {

  @Mock private LoadServiceOrderByIdRepository loadServiceOrderByIdRepository;
  @Mock private UpdateServiceOrderStatusRepository updateServiceOrderStatusRepository;

  private DbStartServiceOrder sut;
  private StartServiceOrder.Params params;
  private ServiceOrder waitingServiceOrder;

  @BeforeEach
  void setUp() {
    sut =
        new DbStartServiceOrder(loadServiceOrderByIdRepository, updateServiceOrderStatusRepository);

    UUID serviceOrderId = UUID.randomUUID();
    UUID washerId = UUID.randomUUID();
    params = new StartServiceOrder.Params(serviceOrderId, washerId);
    Instant now = Instant.parse("2026-09-20T10:00:00Z");
    waitingServiceOrder =
        new ServiceOrder(
            serviceOrderId,
            UUID.randomUUID(),
            1,
            null,
            ServiceOrderStatus.WAITING_IN_YARD,
            now,
            now,
            now);
  }

  @Test
  void movesToWashingAndAssignsWasherWhenServiceOrderIsWaitingInYard() throws Exception {
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.of(waitingServiceOrder));
    ServiceOrder washingServiceOrder =
        new ServiceOrder(
            waitingServiceOrder.id(),
            waitingServiceOrder.vehicleId(),
            waitingServiceOrder.servicePriceId(),
            params.washerId(),
            ServiceOrderStatus.WASHING,
            waitingServiceOrder.scheduledAt(),
            waitingServiceOrder.createdAt(),
            Instant.now());
    when(updateServiceOrderStatusRepository.updateStatus(
            params.serviceOrderId(), ServiceOrderStatus.WASHING, params.washerId()))
        .thenReturn(washingServiceOrder);

    ServiceOrder result = sut.start(params);

    assertEquals(ServiceOrderStatus.WASHING, result.status());
    assertEquals(params.washerId(), result.washerId());
    verify(updateServiceOrderStatusRepository)
        .updateStatus(params.serviceOrderId(), ServiceOrderStatus.WASHING, params.washerId());
  }

  @Test
  void throwsServiceOrderNotFoundWhenServiceOrderDoesNotExist() {
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.empty());

    assertThrows(ServiceOrderNotFoundError.class, () -> sut.start(params));
    verifyNoInteractions(updateServiceOrderStatusRepository);
  }

  @Test
  void throwsInvalidServiceOrderStatusWhenNotWaitingInYard() {
    ServiceOrder alreadyWashing =
        new ServiceOrder(
            waitingServiceOrder.id(),
            waitingServiceOrder.vehicleId(),
            waitingServiceOrder.servicePriceId(),
            UUID.randomUUID(),
            ServiceOrderStatus.WASHING,
            waitingServiceOrder.scheduledAt(),
            waitingServiceOrder.createdAt(),
            waitingServiceOrder.updatedAt());
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.of(alreadyWashing));

    assertThrows(InvalidServiceOrderStatusError.class, () -> sut.start(params));
    verifyNoInteractions(updateServiceOrderStatusRepository);
  }
}
