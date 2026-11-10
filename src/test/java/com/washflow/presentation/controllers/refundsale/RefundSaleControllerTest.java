package com.washflow.presentation.controllers.refundsale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.PaymentMethod;
import com.washflow.domain.entities.Sale;
import com.washflow.domain.errors.sale.SaleNotFoundError;
import com.washflow.domain.errors.sale.SaleRefundNotAllowedError;
import com.washflow.domain.usecases.RefundSale;
import com.washflow.presentation.protocols.HttpRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundSaleControllerTest {

  @Mock private RefundSale refundSale;
  @Mock private Sale sale;

  private RefundSaleController sut;
  private final UUID saleId = UUID.randomUUID();
  private final UUID managerId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    sut = new RefundSaleController(refundSale);
  }

  @Test
  void refundsOnBehalfOfTheAuthenticatedManager() throws Exception {
    when(refundSale.refund(any())).thenReturn(sale);

    var response = sut.handle(request(saleId.toString(), body("pix", 12.5)));

    assertEquals(200, response.statusCode());
    assertEquals(sale, response.body());
    verify(refundSale)
        .refund(
            new RefundSale.Params(
                saleId,
                managerId,
                PaymentMethod.PIX,
                new BigDecimal("12.5"),
                "WRONG_CHARGE",
                null));
  }

  @Test
  void rejectsInvalidIdMethodOrAmount() {
    Map<String, Object> textAmount = body("PIX", 1);
    textAmount.put("amount", "dez");

    assertEquals(400, sut.handle(request("x", body("PIX", 1))).statusCode());
    assertEquals(400, sut.handle(request(saleId.toString(), body("BITCOIN", 1))).statusCode());
    assertEquals(400, sut.handle(request(saleId.toString(), textAmount)).statusCode());
    verifyNoInteractions(refundSale);
  }

  @Test
  void mapsNotFoundAndNotAllowed() throws Exception {
    when(refundSale.refund(any()))
        .thenThrow(new SaleNotFoundError())
        .thenThrow(new SaleRefundNotAllowedError("Orçamento não tem estorno"));

    assertEquals(404, sut.handle(request(saleId.toString(), body("PIX", 1))).statusCode());
    assertEquals(409, sut.handle(request(saleId.toString(), body("PIX", 1))).statusCode());
  }

  private Map<String, Object> body(String method, Number amount) {
    Map<String, Object> body = new HashMap<>();
    body.put("method", method);
    body.put("amount", amount);
    body.put("reason", "WRONG_CHARGE");
    body.put("createdBy", managerId);
    return body;
  }

  private static HttpRequest request(String id, Map<String, Object> body) {
    return new HttpRequest(body, Map.of("id", id), Map.of());
  }
}
