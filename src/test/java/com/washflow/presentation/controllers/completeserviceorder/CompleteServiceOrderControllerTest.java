package com.washflow.presentation.controllers.completeserviceorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.ServiceOrder;
import com.washflow.domain.entities.ServiceOrderStatus;
import com.washflow.domain.errors.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.CompleteServiceOrder;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompleteServiceOrderControllerTest {

  @Mock private CompleteServiceOrder completeServiceOrder;

  private CompleteServiceOrderController sut;
  private UUID serviceOrderId;

  @BeforeEach
  void setUp() {
    sut = new CompleteServiceOrderController(completeServiceOrder);
    serviceOrderId = UUID.randomUUID();
  }

  @Test
  void completesServiceOrderAndReturnsOk() throws Exception {
    HttpRequest request =
        new HttpRequest(Map.of(), Map.of("id", serviceOrderId.toString()), Map.of());

    ServiceOrder saved =
        new ServiceOrder(
            serviceOrderId,
            UUID.randomUUID(),
            1,
            UUID.randomUUID(),
            ServiceOrderStatus.READY,
            Instant.now(),
            Instant.now(),
            Instant.now());
    when(completeServiceOrder.complete(any(CompleteServiceOrder.Params.class))).thenReturn(saved);

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(completeServiceOrder).complete(any(CompleteServiceOrder.Params.class));
  }

  @Test
  void returnsBadRequestWhenServiceOrderIdIsInvalid() {
    HttpRequest request = new HttpRequest(Map.of(), Map.of("id", "not-a-uuid"), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsNotFoundWhenServiceOrderNotFound() throws Exception {
    HttpRequest request =
        new HttpRequest(Map.of(), Map.of("id", serviceOrderId.toString()), Map.of());

    when(completeServiceOrder.complete(any(CompleteServiceOrder.Params.class)))
        .thenThrow(new ServiceOrderNotFoundError(serviceOrderId));

    HttpResponse response = sut.handle(request);

    assertEquals(404, response.statusCode());
  }

  @Test
  void returnsConflictWhenInvalidStatus() throws Exception {
    HttpRequest request =
        new HttpRequest(Map.of(), Map.of("id", serviceOrderId.toString()), Map.of());

    when(completeServiceOrder.complete(any(CompleteServiceOrder.Params.class)))
        .thenThrow(
            new InvalidServiceOrderStatusError(
                ServiceOrderStatus.WASHING, ServiceOrderStatus.READY));

    HttpResponse response = sut.handle(request);

    assertEquals(409, response.statusCode());
  }
}
