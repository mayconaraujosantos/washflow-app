package com.washflow.presentation.controllers.getyardoverview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.usecases.GetYardOverview;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetYardOverviewControllerTest {

  @Mock private GetYardOverview getYardOverview;

  private GetYardOverviewController sut;

  @BeforeEach
  void setUp() {
    sut = new GetYardOverviewController(getYardOverview);
  }

  @Test
  void getsYardOverviewAndReturnsOk() throws Exception {
    HttpRequest request = new HttpRequest(Map.of(), Map.of(), Map.of());

    List<ServiceOrder> yard =
        List.of(
            new ServiceOrder(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                null,
                ServiceOrderStatus.WAITING_IN_YARD,
                Instant.now(),
                Instant.now(),
                Instant.now()),
            new ServiceOrder(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                UUID.randomUUID(),
                ServiceOrderStatus.WASHING,
                Instant.now(),
                Instant.now(),
                Instant.now()));
    when(getYardOverview.get()).thenReturn(yard);

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(getYardOverview).get();
  }

  @Test
  void returnsEmptyListWhenNoYard() throws Exception {
    HttpRequest request = new HttpRequest(Map.of(), Map.of(), Map.of());

    when(getYardOverview.get()).thenReturn(List.of());

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
  }
}
