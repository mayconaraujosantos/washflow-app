package com.washflow.data.usecases.getserviceorderbyid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.washflow.data.protocols.db.LoadServiceOrderByIdRepository;
import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.GetServiceOrderById;
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
class DbGetServiceOrderByIdTest {

  @Mock private LoadServiceOrderByIdRepository loadServiceOrderByIdRepository;

  private DbGetServiceOrderById sut;
  private GetServiceOrderById.Params params;

  @BeforeEach
  void setUp() {
    sut = new DbGetServiceOrderById(loadServiceOrderByIdRepository);
    params = new GetServiceOrderById.Params(UUID.randomUUID());
  }

  @Test
  void returnsServiceOrderWhenFound() throws Exception {
    Instant now = Instant.parse("2026-09-20T10:00:00Z");
    ServiceOrder serviceOrder =
        new ServiceOrder(
            params.serviceOrderId(),
            UUID.randomUUID(),
            1,
            null,
            ServiceOrderStatus.WASHING,
            now,
            now,
            now);
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.of(serviceOrder));

    ServiceOrder result = sut.get(params);

    assertEquals(serviceOrder, result);
  }

  @Test
  void throwsServiceOrderNotFoundWhenMissing() {
    when(loadServiceOrderByIdRepository.loadById(params.serviceOrderId()))
        .thenReturn(Optional.empty());

    assertThrows(ServiceOrderNotFoundError.class, () -> sut.get(params));
  }
}
