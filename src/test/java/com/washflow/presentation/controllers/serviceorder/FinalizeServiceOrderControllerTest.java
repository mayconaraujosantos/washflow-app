package com.washflow.presentation.controllers.serviceorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.serviceorder.ServiceOrder;
import com.washflow.domain.entities.serviceorder.ServiceOrderStatus;
import com.washflow.domain.errors.serviceorder.InvalidServiceOrderStatusError;
import com.washflow.domain.errors.serviceorder.ServiceOrderNotFoundError;
import com.washflow.domain.usecases.serviceorder.FinalizeServiceOrder;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinalizeServiceOrderControllerTest {

  @Mock private FinalizeServiceOrder finalizeServiceOrder;

  private FinalizeServiceOrderController sut;
  private UUID serviceOrderId;

  @BeforeEach
  void setUp() {
    sut = new FinalizeServiceOrderController(finalizeServiceOrder);
    serviceOrderId = UUID.randomUUID();
  }

  @Test
  void finalizesServiceOrderAndReturnsOk() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("paymentMethod", "PIX");
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()), Map.of());

    ServiceOrder saved =
        new ServiceOrder(
            serviceOrderId,
            UUID.randomUUID(),
            1,
            UUID.randomUUID(),
            ServiceOrderStatus.DONE,
            Instant.now(),
            Instant.now(),
            Instant.now());
    when(finalizeServiceOrder.finalizeOrder(any(FinalizeServiceOrder.Params.class)))
        .thenReturn(saved);

    HttpResponse response = sut.handle(request);

    assertEquals(200, response.statusCode());
    verify(finalizeServiceOrder).finalizeOrder(any(FinalizeServiceOrder.Params.class));
  }

  @Test
  void returnsBadRequestWhenPaymentMethodIsMissing() {
    Map<String, Object> body = new HashMap<>();
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsBadRequestWhenServiceOrderIdIsInvalid() {
    Map<String, Object> body = new HashMap<>();
    body.put("paymentMethod", "PIX");
    HttpRequest request = new HttpRequest(body, Map.of("id", "not-a-uuid"), Map.of());

    HttpResponse response = sut.handle(request);

    assertEquals(400, response.statusCode());
  }

  @Test
  void returnsNotFoundWhenServiceOrderNotFound() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("paymentMethod", "PIX");
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()), Map.of());

    when(finalizeServiceOrder.finalizeOrder(any(FinalizeServiceOrder.Params.class)))
        .thenThrow(new ServiceOrderNotFoundError(serviceOrderId));

    HttpResponse response = sut.handle(request);

    assertEquals(404, response.statusCode());
  }

  @Test
  void returnsConflictWhenInvalidStatus() throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("paymentMethod", "PIX");
    HttpRequest request = new HttpRequest(body, Map.of("id", serviceOrderId.toString()), Map.of());

    when(finalizeServiceOrder.finalizeOrder(any(FinalizeServiceOrder.Params.class)))
        .thenThrow(
            new InvalidServiceOrderStatusError(
                ServiceOrderStatus.SCHEDULED, ServiceOrderStatus.READY));

    HttpResponse response = sut.handle(request);

    assertEquals(409, response.statusCode());
  }
}
