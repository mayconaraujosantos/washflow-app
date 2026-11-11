package com.washflow.presentation.controllers.sale;

import static com.washflow.presentation.helpers.HttpRequests.body;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.washflow.domain.entities.sale.SaleType;
import com.washflow.domain.entities.shared.PaymentMethod;
import com.washflow.domain.errors.finance.ServicePriceNotFoundError;
import com.washflow.domain.usecases.sale.CreateSale;
import com.washflow.presentation.protocols.HttpRequest;
import com.washflow.presentation.protocols.HttpResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateSaleControllerTest {

  @Test
  void registersAQuickSaleWithItemsAndPayments() throws Exception {
    CreateSale create = mock(CreateSale.class);
    UUID manager = UUID.randomUUID();
    HttpRequest request =
        body(
            "type",
            "quick_sale",
            "items",
            List.of(Map.of("servicePriceId", 1, "quantity", 2)),
            "discount",
            1,
            "payments",
            List.of(Map.of("method", "pix", "amount", 49.5)),
            "notes",
            "Nota",
            "createdBy",
            manager);

    HttpResponse response = new CreateSaleController(create).handle(request);

    assertEquals(200, response.statusCode());
    verify(create)
        .create(
            new CreateSale.Params(
                SaleType.QUICK_SALE,
                null,
                null,
                null,
                List.of(new CreateSale.Item(1, 2)),
                new BigDecimal("1"),
                List.of(new CreateSale.Payment(PaymentMethod.PIX, new BigDecimal("49.5"))),
                "Nota",
                manager));
  }

  @Test
  void rejectsMissingTypeWrongItemsAndWrongTypes() {
    CreateSale create = mock(CreateSale.class);
    CreateSaleController sut = new CreateSaleController(create);

    assertEquals(400, sut.handle(body()).statusCode());
    assertEquals(400, sut.handle(body("type", "BUDGET", "items", List.of("x"))).statusCode());
    assertEquals(
        400,
        sut.handle(
                body(
                    "type",
                    "BUDGET",
                    "items",
                    List.of(Map.of("servicePriceId", "1", "quantity", 1))))
            .statusCode());
    assertEquals(400, sut.handle(body("type", "BUDGET", "customerName", 5)).statusCode());
    verifyNoInteractions(create);
  }

  @Test
  void anUnknownServiceIsNotFound() throws Exception {
    CreateSale create = mock(CreateSale.class);
    when(create.create(any())).thenThrow(new ServicePriceNotFoundError(9));

    assertEquals(
        404,
        new CreateSaleController(create)
            .handle(
                body(
                    "type", "BUDGET", "items", List.of(Map.of("servicePriceId", 9, "quantity", 1))))
            .statusCode());
  }
}
