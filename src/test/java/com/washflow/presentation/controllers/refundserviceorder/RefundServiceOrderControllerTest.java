package com.washflow.presentation.controllers.refundserviceorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.ServiceOrderRefund;
import com.washflow.domain.errors.ServiceOrderNotRefundableError;
import com.washflow.domain.usecases.RefundServiceOrder;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.math.BigDecimal;
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
class RefundServiceOrderControllerTest {

  @Mock private RefundServiceOrder refundServiceOrder;

  private RefundServiceOrderController sut;
  private final UUID orderId = UUID.randomUUID();
  private final UUID managerId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    sut = new RefundServiceOrderController(refundServiceOrder);
  }

  @Test
  void refundsOnBehalfOfTheAuthenticatedManager() throws Exception {
    ServiceOrderRefund refund =
        new ServiceOrderRefund(
            UUID.randomUUID(),
            orderId,
            new BigDecimal("60.00"),
            PaymentMethod.CASH,
            "WRONG_CHARGE",
            null,
            new BigDecimal("20.00"),
            managerId,
            Instant.now());
    when(refundServiceOrder.refund(any())).thenReturn(refund);
    Map<String, Object> body = body("WRONG_CHARGE");
    body.put("refundMethod", "cash");
    body.put("reverseCommission", true);

    HttpResponse response = sut.handle(request(orderId.toString(), body));

    assertEquals(200, response.statusCode());
    assertEquals(refund, response.body());
    verify(refundServiceOrder)
        .refund(
            new RefundServiceOrder.Params(
                orderId, managerId, "WRONG_CHARGE", null, PaymentMethod.CASH, true));
  }

  @Test
  void defaultsToTheOriginalMethodAndKeepingTheCommission() throws Exception {
    sut.handle(request(orderId.toString(), body("WRONG_CHARGE")));

    verify(refundServiceOrder)
        .refund(
            new RefundServiceOrder.Params(orderId, managerId, "WRONG_CHARGE", null, null, false));
  }

  @Test
  void rejectsAnInvalidIdOrMethod() {
    Map<String, Object> badMethod = body("WRONG_CHARGE");
    badMethod.put("refundMethod", "BITCOIN");

    assertEquals(400, sut.handle(request("x", body("WRONG_CHARGE"))).statusCode());
    assertEquals(400, sut.handle(request(orderId.toString(), badMethod)).statusCode());
    verifyNoInteractions(refundServiceOrder);
  }

  @Test
  void conflictsWhenTheOrderIsNotFinalized() throws Exception {
    doThrow(new ServiceOrderNotRefundableError()).when(refundServiceOrder).refund(any());

    assertEquals(409, sut.handle(request(orderId.toString(), body("WRONG_CHARGE"))).statusCode());
  }

  private Map<String, Object> body(String reason) {
    Map<String, Object> body = new HashMap<>();
    body.put("reason", reason);
    body.put("createdBy", managerId);
    return body;
  }

  private static HttpRequest request(String id, Map<String, Object> body) {
    return new HttpRequest(body, Map.of("id", id), Map.of());
  }
}
