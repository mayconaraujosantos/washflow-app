package com.washflow.data.usecases.completeserviceorder;

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
import com.washflow.domain.usecases.CompleteServiceOrder;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Same payoff as {@code DbStartServiceOrderTest}: exercised here with plain Mockito doubles, no
 * Postgres/JDBI/Testcontainers involved at all.
 */
@ExtendWith(MockitoExtension.class)
class DbCompleteServiceOrderTest {

  @Mock private LoadServiceOrderByIdRepository loadServiceOrderByIdRepository;
  @Mock private UpdateServiceOrderStatusRepository updateServiceOrderStatusRepository;

  private DbCompleteServiceOrder sut;
  private CompleteServiceOrder.Params params;
  private ServiceOrder washingServiceOrder;

  @BeforeEach
  void setUp() {
    sut =
        new DbCompleteServiceOrder(
            loadServiceOrderByIdRepository, updateServiceOrderStatusRepository);

    UUID serviceOrderId = UUID.randomUUID();
    UUID washerId = UUID.randomUUID();
    params = new CompleteServiceOrder.Params(serviceOrderId);
    Instant now = Instant.parse("2026-09-20T10:00:00Z");
    washingServiceOrder =
        new ServiceOrder(
            serviceOrderId,
            UUID.randomUUID(),
            1,
            washerId,
            ServiceOrderStatus.WASHING,
            now,
            now,
            now);
  }

  @Test
  void movesToReadyWhenServiceOrderIsWashing() throws Exception {
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.of(washingServiceOrder));
    ServiceOrder readyServiceOrder =
        new ServiceOrder(
            washingServiceOrder.id(),
            washingServiceOrder.vehicleId(),
            washingServiceOrder.servicePriceId(),
            washingServiceOrder.washerId(),
            ServiceOrderStatus.READY,
            washingServiceOrder.scheduledAt(),
            washingServiceOrder.createdAt(),
            Instant.now());
    when(updateServiceOrderStatusRepository.updateStatus(
            params.serviceOrderId(), ServiceOrderStatus.READY, washingServiceOrder.washerId()))
        .thenReturn(readyServiceOrder);

    ServiceOrder result = sut.complete(params);

    assertEquals(ServiceOrderStatus.READY, result.status());
    verify(updateServiceOrderStatusRepository)
        .updateStatus(
            params.serviceOrderId(), ServiceOrderStatus.READY, washingServiceOrder.washerId());
  }

  @Test
  void throwsServiceOrderNotFoundWhenServiceOrderDoesNotExist() {
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.empty());

    assertThrows(ServiceOrderNotFoundError.class, () -> sut.complete(params));
    verifyNoInteractions(updateServiceOrderStatusRepository);
  }

  @Test
  void throwsInvalidServiceOrderStatusWhenNotWashing() {
    ServiceOrder alreadyReady =
        new ServiceOrder(
            washingServiceOrder.id(),
            washingServiceOrder.vehicleId(),
            washingServiceOrder.servicePriceId(),
            washingServiceOrder.washerId(),
            ServiceOrderStatus.READY,
            washingServiceOrder.scheduledAt(),
            washingServiceOrder.createdAt(),
            washingServiceOrder.updatedAt());
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.of(alreadyReady));

    assertThrows(InvalidServiceOrderStatusError.class, () -> sut.complete(params));
    verifyNoInteractions(updateServiceOrderStatusRepository);
  }
}
