package com.washflow.presentation.controllers.cancelserviceorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.washflow.domain.errors.serviceorder.ServiceOrderNotCancellableError;
import com.washflow.domain.usecases.CancelServiceOrder;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelServiceOrderControllerTest {

  @Mock private CancelServiceOrder cancelServiceOrder;

  private CancelServiceOrderController sut;
  private final UUID orderId = UUID.randomUUID();
  private final UUID managerId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    sut = new CancelServiceOrderController(cancelServiceOrder);
  }

  @Test
  void cancelsOnBehalfOfTheAuthenticatedManager() throws Exception {
    HttpResponse response = sut.handle(request(orderId.toString(), "NO_SHOW", null));

    assertEquals(200, response.statusCode());
    verify(cancelServiceOrder)
        .cancel(new CancelServiceOrder.Params(orderId, managerId, "NO_SHOW", null));
  }

  @Test
  void rejectsAnInvalidId() {
    assertEquals(400, sut.handle(request("x", "NO_SHOW", null)).statusCode());
  }

  @Test
  void rejectsAnInvalidReason() throws Exception {
    doThrow(new IllegalArgumentException("motivo"))
        .when(cancelServiceOrder)
        .cancel(any(CancelServiceOrder.Params.class));

    assertEquals(400, sut.handle(request(orderId.toString(), "ANY", null)).statusCode());
  }

  @Test
  void conflictsWhenTheOrderCanNoLongerBeCancelled() throws Exception {
    doThrow(new ServiceOrderNotCancellableError())
        .when(cancelServiceOrder)
        .cancel(any(CancelServiceOrder.Params.class));

    assertEquals(409, sut.handle(request(orderId.toString(), "NO_SHOW", null)).statusCode());
  }

  private HttpRequest request(String id, String reason, String description) {
    Map<String, Object> body = new HashMap<>();
    body.put("reason", reason);
    body.put("description", description);
    body.put("createdBy", managerId);
    return new HttpRequest(body, Map.of("id", id), Map.of());
  }
}
